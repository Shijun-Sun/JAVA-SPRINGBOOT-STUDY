# M02-05 · 事务、隔离级别与 MVCC：并发下读写正确性的地基

> **预计阅读时间**：150～180 分钟（本章是 M02 最硬的一块，慢一点没关系）
> **配套作业**：[作业 05 · 事务、隔离级别与 MVCC 实战](../作业/05-事务隔离级别与MVCC实战.md)
> **版本基线**：MySQL 8.4 LTS、InnoDB、默认隔离级别 **REPEATABLE READ（RR）**、`utf8mb4`
> **前置**：[M02-02 · InnoDB 存储结构与 B+ 树](../../02-InnoDB存储结构与B+树/学习/02-InnoDB存储结构与B+树.md)（本章用到聚簇索引、隐藏列、undo log）、[M02-04 · EXPLAIN 与慢查询优化](../../04-EXPLAIN与慢查询优化/学习/04-EXPLAIN与慢查询优化.md)

> **本章目标**
> - 说清事务的 ACID 四性，以及每一性在 InnoDB 里**靠什么机制兜底**（redo log / undo log / 锁 / 约束）
> - 讲清三类并发读问题：**脏读、不可重复读、幻读**，各自的定义和真实例子
> - 讲清四种隔离级别各自"防住了什么、放过了什么"，以及 MySQL 为什么默认 **RR**
> - **吃透 MVCC**：隐藏列（`DB_TRX_ID`/`DB_ROLL_PTR`）、undo log 版本链、ReadView 可见性判断
> - 分清 **快照读（普通 SELECT）** 和 **当前读（`SELECT ... FOR UPDATE` / `UPDATE`）**，知道电商扣库存该用哪个
> - 知道 RR 下"幻读被解决到什么程度"，为第 06 章的锁埋好伏笔

---

## 一、我们在解决什么问题：单条 SQL 对，不代表并发对

04 章我们让单条 SQL 跑得快。但真实电商系统里，**成百上千个请求同时在读写同一批数据**：两个人同时下单抢最后一件库存、后台改价格的同时用户正在下单、对账任务在统计的同时订单还在变。

一条一条看每个操作都对，**放到一起并发跑，就可能读到别人没提交的脏数据、同一笔查询前后结果不一致、明明查了没有却插入失败**。事务和隔离级别，就是数据库给并发正确性提供的地基。

一句话定位本章在 M02 里的位置：

- 02 章：数据**怎么存**（页、B+ 树、undo log 这些结构，本章要复用）
- 03/04 章：数据**怎么查得快**（索引、执行计划）
- **05 章（本章）：并发读写下，怎么保证读到的、写下去的是"正确"的**
- 06 章：为了保证正确，InnoDB 到底**加了哪些锁**（行锁、间隙锁、Next-Key）

事务是"正确性"的入口，锁是"正确性"的实现手段之一，MVCC 是"让读不加锁也能正确"的核心机制。本章把前两块讲透，锁的细节留到第 06 章。

---

## 二、事务与 ACID：四个承诺，各由谁兜底 ⭐

**事务（Transaction）**：一组要么全部成功、要么全部不生效的操作，是并发控制和故障恢复的基本单位。

```sql
START TRANSACTION;          -- 或 BEGIN
UPDATE sku SET stock = stock - 1 WHERE id = 100 AND stock > 0;
INSERT INTO `order` (...) VALUES (...);
COMMIT;                     -- 成功；出错则 ROLLBACK 全部撤销
```

ACID 是事务的四个承诺。**关键不是背定义，而是知道每一性靠什么机制实现**——这是面试和架构判断的分水岭：

| 特性 | 含义 | InnoDB 靠什么实现 |
|------|------|------------------|
| **A** 原子性 Atomicity | 事务内操作要么全做、要么全不做 | **undo log**（回滚日志）：出错时用它把已做的改动逆向撤销 |
| **C** 一致性 Consistency | 事务前后数据满足业务约束（钱不凭空增减） | **A+I+D 的结果 + 约束**（主键/唯一/外键/CHECK）+ 应用逻辑，是"目的"而非单一机制 |
| **I** 隔离性 Isolation | 并发事务互不干扰，像串行执行 | **锁 + MVCC**（本章重点） |
| **D** 持久性 Durability | 一旦提交，宕机也不丢 | **redo log**（重做日志）+ 刷盘策略 |

三个"log/机制"的分工，02 章打过底，这里连起来：

- **undo log**：记录"改之前长什么样"，用于**回滚**（原子性）和 **MVCC 读旧版本**（隔离性）。本章的 MVCC 版本链就是它。
- **redo log**：记录"改成了什么"，用于**崩溃恢复**（持久性）。提交时先落 redo，脏页可以晚点刷。
- **锁**：控制**当前读/写**的并发（隔离性），第 06 章展开。

> 一致性（C）常被误解成"和其它三性并列的机制"。更准确的理解：**A、I、D 是手段，C 是目的**——原子性保证不会做一半，隔离性保证并发不互相污染，持久性保证结果不丢，再加上数据库约束和正确的业务逻辑，最终让数据始终满足业务规则。

### 2.1 事务的边界与自动提交

MySQL 默认 `autocommit = ON`：每条单独的 SQL 都是一个自动提交的事务。显式事务用 `BEGIN`/`COMMIT` 把多条包起来。

```sql
SHOW VARIABLES LIKE 'autocommit';   -- 默认 ON
SELECT @@transaction_isolation;     -- 看当前隔离级别，默认 REPEATABLE-READ
```

> ⚠️ 在 Spring 里，事务边界由 `@Transactional` 控制（底层就是帮你 `BEGIN`/`COMMIT`/`ROLLBACK`），传播行为、回滚规则等第 08 章持久层集成时细讲。本章先在**裸 SQL 层**把机制看清楚，才不会被框架的"魔法"糊住眼睛。

---

## 三、并发带来的三类读问题 ⭐⭐

不加任何隔离，两个事务并发读写同一行，会出现三类经典问题。**记住定义时抓住"读到了什么不该读的"**：

### 3.1 脏读（Dirty Read）：读到别人**没提交**的改动

事务 B 改了某行但**还没提交**，事务 A 读到了这个中间值。随后 B 回滚，A 手里的数据就是从没存在过的"脏"数据。

```
时间轴：
T1  A: 查 sku=100 库存 = 10
T2  B: BEGIN; UPDATE sku SET stock=0 WHERE id=100;   -- 未提交
T3  A: 查 sku=100 库存 = 0     ← 读到了 B 未提交的值（脏读）
T4  B: ROLLBACK;               -- B 反悔，库存其实还是 10
    A 基于"库存=0"做的判断全错了
```

### 3.2 不可重复读（Non-Repeatable Read）：同一行，两次读值不同

事务 A 内**两次读同一行**，中间事务 B 改了这行并**提交**，导致 A 前后读到的值不一样。问题出在 **UPDATE**（值变了）。

```
T1  A: BEGIN; 查 sku=100 库存 = 10
T2  B: UPDATE sku SET stock=5 WHERE id=100; COMMIT;   -- 已提交
T3  A: 再查 sku=100 库存 = 5    ← 同一事务内两次读值不同
```

### 3.3 幻读（Phantom Read）：同一范围，两次读**行数**不同

事务 A 内两次按**同一范围条件**查询，中间事务 B **插入（或删除）**了符合该范围的行并提交，A 第二次读多出（或少了）"幻影行"。问题出在 **INSERT/DELETE**（行数变了）。

```
T1  A: BEGIN; SELECT COUNT(*) FROM spu WHERE category_id=3;  -- 5 行
T2  B: INSERT INTO spu(category_id,...) VALUES(3,...); COMMIT;
T3  A: SELECT COUNT(*) FROM spu WHERE category_id=3;  -- 6 行 ← 多出"幻影行"
```

> **不可重复读 vs 幻读的区别**（高频面试题）：
> - 不可重复读关注**同一行的值变了**（UPDATE），解决靠"锁住这一行"或 MVCC 快照。
> - 幻读关注**满足条件的行数变了**（INSERT/DELETE），解决要"锁住一个范围"（间隙锁，第 06 章），单靠锁已存在的行不够，因为新行本来不存在、锁不住。

---

## 四、四种隔离级别：防住什么、放过什么 ⭐⭐

SQL 标准定义四级隔离，级别越高越安全、并发度越低。**用"允许哪些问题发生"来记最清楚**：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 说明 |
|---------|------|-----------|------|------|
| **READ UNCOMMITTED** 读未提交 | ✅ 可能 | ✅ 可能 | ✅ 可能 | 几乎不用，能读到未提交数据 |
| **READ COMMITTED** 读已提交（RC） | ❌ 防住 | ✅ 可能 | ✅ 可能 | Oracle/PG 默认；只读已提交的 |
| **REPEATABLE READ** 可重复读（RR） | ❌ | ❌ 防住 | ⚠️ 基本防住 | **MySQL 默认** |
| **SERIALIZABLE** 串行化 | ❌ | ❌ | ❌ 全防 | 事务串行执行，最安全、最慢 |

✅ = 该级别下这个问题**仍可能发生**；❌ = 已防住。

### 4.1 MySQL 为什么默认 RR，而不是 RC

多数数据库（Oracle、PostgreSQL）默认 RC，MySQL 却默认 **RR**，历史原因和一个关键技术点：

- **历史**：早期 MySQL 主从复制基于 **statement（语句）格式的 binlog**。RC 级别下，事务提交顺序和语句执行顺序可能不一致，statement 复制会导致主从数据不一致。RR 能保证主从一致，于是成了默认。
- **RR 的强化**：InnoDB 在 RR 下不仅防住不可重复读，还通过 **MVCC（快照读）+ Next-Key Lock（当前读）** 把幻读也**基本解决**了——这是 InnoDB 比 SQL 标准更强的地方（标准里 RR 是允许幻读的）。

> ⚠️ "基本解决"要拆开看（第七节细讲）：
> - **快照读**（普通 `SELECT`）：靠 MVCC，整个事务读同一个快照，天然不会幻读。
> - **当前读**（`SELECT ... FOR UPDATE`、`UPDATE`、`DELETE`）：靠 Next-Key Lock 锁住范围，阻止别的事务往范围里插入，从而防幻读。
> - 但**"快照读 + 当前读混用"**仍能构造出幻读的边角案例（作业里会让你亲手复现），所以说"基本"而非"绝对"。

### 4.2 查看与设置隔离级别

```sql
-- 查看（会话级 / 全局级）
SELECT @@transaction_isolation;         -- 当前会话
SELECT @@global.transaction_isolation;  -- 全局

-- 设置（三种作用域）
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;   -- 当前会话后续事务
SET GLOBAL  TRANSACTION ISOLATION LEVEL READ COMMITTED;   -- 之后的新连接
SET TRANSACTION ISOLATION LEVEL READ COMMITTED;           -- 只影响下一个事务
```

> `SET GLOBAL` 只对**新连接**生效，当前连接不变；重启后丢失，永久生效要写配置文件。作业里会用两个会话分别设不同隔离级别做对比实验。

---

## 五、MVCC：让"读不加锁"也能正确 ⭐⭐⭐

MVCC（Multi-Version Concurrency Control，多版本并发控制）是本章的核心，也是 InnoDB 高并发的关键。它的目标是：**读操作不加锁、不阻塞写；写操作也不阻塞读**——读写不互相等待，靠"读一个合适的历史版本"实现。

理解 MVCC 只需要三样东西：**隐藏列、undo log 版本链、ReadView**。

### 5.1 每行的隐藏列

InnoDB 给每行额外存了隐藏列（02 章提过），MVCC 用到两个：

| 隐藏列 | 含义 |
|--------|------|
| **`DB_TRX_ID`** | 最近**修改**（insert/update）这行的事务 id。事务 id 全局递增。 |
| **`DB_ROLL_PTR`** | 回滚指针，指向这行在 undo log 里的**上一个版本**，串成版本链 |
| `DB_ROW_ID` | 无主键时自动生成的行 id（和 MVCC 关系不大） |

### 5.2 undo log 版本链

每次 `UPDATE` 一行，旧值不是被直接覆盖丢弃，而是**写进 undo log**，新行的 `DB_ROLL_PTR` 指向旧版本。多次修改就串成一条**版本链**（从新到旧）：

```
当前行(trx_id=30) ──roll_ptr──► 旧版本(trx_id=20) ──roll_ptr──► 更旧(trx_id=10)
  stock=5                         stock=8                          stock=10
```

这条链就是"同一行的多个历史版本"。**读的时候，MVCC 沿着链找到"对当前事务可见"的那个版本**。这也解释了 undo log 为什么既能回滚（原子性）又能支撑一致性读（隔离性）——它本来就存着历史版本。

### 5.3 ReadView：判断"哪个版本对我可见" ⭐

**ReadView（读视图 / 一致性视图）** 是某个时刻的"活跃事务快照"，决定当前事务能看到版本链上的哪个版本。ReadView 里关键字段：

| 字段 | 含义 |
|------|------|
| `m_ids` | 生成 ReadView 时，**还活跃（未提交）**的事务 id 列表 |
| `min_trx_id` | `m_ids` 里的最小值 |
| `max_trx_id` | 下一个将分配的事务 id（当前最大 id + 1） |
| `creator_trx_id` | 创建这个 ReadView 的事务自己的 id |

**可见性判断规则**（拿版本的 `DB_TRX_ID` 逐条比对，记住"提交在前才可见"）：

1. `trx_id == creator_trx_id`：这版本是**我自己改的**，可见。
2. `trx_id < min_trx_id`：生成 ReadView 前，这事务**早就提交了**，可见。
3. `trx_id >= max_trx_id`：这事务是**我之后才开始的**，不可见（顺链找更旧版本）。
4. `min_trx_id <= trx_id < max_trx_id`：
   - 若 `trx_id` **在 `m_ids` 里**（生成视图时还活跃/未提交）→ **不可见**，顺链找旧版本；
   - 若**不在 `m_ids` 里**（说明已提交）→ 可见。

命中"不可见"就沿 `DB_ROLL_PTR` 往更旧的版本走，直到找到可见版本（或到链尾）。**整个过程不加锁**，这就是"快照读"。

### 5.4 RC 和 RR 的唯一区别：ReadView 什么时候建 ⭐⭐

同一套 MVCC 机制，RC 和 RR 表现不同，**差别只在生成 ReadView 的时机**：

| 隔离级别 | ReadView 生成时机 | 效果 |
|---------|------------------|------|
| **RC 读已提交** | **每次快照读都新建**一个 ReadView | 每次 `SELECT` 都看最新已提交数据 → 会不可重复读（但读不到未提交，没脏读） |
| **RR 可重复读** | **事务内第一次快照读**时建，之后**整个事务复用** | 整个事务看同一个快照 → 同一行反复读值不变，不可重复读被防住 |

这一句是理解隔离级别的钥匙：**RC 每条 SELECT 一个新快照，RR 全事务一个快照。** 记住它，RC/RR 的所有行为差异都能推出来。

> 举例：RR 事务 A 第一次 `SELECT` 建了快照（此时库存 10）。之后 B 改成 5 并提交。A 再查，因为复用老快照、B 的 trx_id 在当时是"未提交/或之后开始"，仍看到 10 → 可重复读。换成 RC，A 第二次 `SELECT` 会新建快照，B 已提交，于是看到 5 → 不可重复读。

---

## 六、快照读 vs 当前读 ⭐⭐⭐

这是本章**最实用、也最容易混**的一对概念，直接决定电商扣库存写得对不对。

### 6.1 快照读（Snapshot Read / 一致性非锁定读）

**普通的 `SELECT`**（不带锁的），走 MVCC，读的是**ReadView 对应的历史快照**，不加锁、不阻塞别人：

```sql
SELECT stock FROM sku WHERE id = 100;   -- 快照读，可能读到的是旧版本
```

优点：读写不互相阻塞，高并发。代价：读到的可能不是"此刻最新"的值（是我快照时刻的值）。

### 6.2 当前读（Current Read / 锁定读）

读取**最新已提交版本**，并且**加锁**，保证读到的就是当下真实值、且读完到事务结束别人不能改。以下都是当前读：

```sql
SELECT ... FOR UPDATE;         -- 加排他锁(X)，最常用于"读了要改"
SELECT ... FOR SHARE;          -- 加共享锁(S)（旧写法 LOCK IN SHARE MODE）
UPDATE / DELETE / INSERT;      -- 写操作本身就是当前读 + 加锁
```

`UPDATE`、`DELETE` 天然是当前读——它们必须基于**最新值**来改，否则会覆盖别人的修改。

### 6.3 电商扣库存：为什么必须用当前读 ⭐

这是把本章知识落到业务的关键案例。假设用快照读判断库存再扣：

```sql
-- ❌ 错误写法：先快照读判断，再扣（并发下会超卖）
SELECT stock FROM sku WHERE id = 100;         -- 快照读，读到 stock=1
-- 应用层判断 stock>0，通过
UPDATE sku SET stock = stock - 1 WHERE id = 100;
```

并发下两个事务都快照读到 `stock=1`、都判断通过、都扣，结果扣成 `-1`，**超卖**。

正确做法是把"判断 + 扣减"交给**一条原子的当前读 UPDATE**，用 `WHERE` 兜住库存：

```sql
-- ✅ 正确：UPDATE 是当前读，读最新值 + 行锁，WHERE 保证不超卖
UPDATE sku SET stock = stock - 1 WHERE id = 100 AND stock > 0;
-- 检查影响行数：affected rows = 1 才算扣成功，= 0 说明库存不足
```

`UPDATE` 是当前读，会锁住这行，第二个事务必须等第一个提交后拿到最新 `stock` 再执行，`stock > 0` 条件天然挡住超卖。**"读了马上要改"的场景，永远别信快照读的值，要用当前读（`FOR UPDATE` 或直接在 `UPDATE` 里带条件）。**

> 更高并发下（秒杀）这样直接打 DB 仍不够，要上 Redis 预扣减、异步下单（M03/M06/M11），但**数据库这一层的正确性底线，就是当前读 + 条件更新**。

---

## 七、RR 下的幻读，到底解决到什么程度 ⭐

回到第四节留的坑。RR 下幻读分两条线看：

- **快照读线**：整个事务复用一个 ReadView，两次范围查询看的是同一快照，别人插入的新行 `trx_id` 在快照里不可见 → **天然不幻读**。
- **当前读线**：`SELECT ... FOR UPDATE` / `UPDATE` 会加 **Next-Key Lock（行锁 + 间隙锁）**，锁住"符合条件的记录 + 记录之间的间隙"，别的事务想往这个范围插入会被阻塞 → **靠锁防幻读**（第 06 章详解间隙锁）。

**能构造出的边角幻读**：一个 RR 事务先快照读（看到 5 行），别的事务插入并提交，本事务再用**当前读**（`FOR UPDATE`）查同一范围——当前读读最新值会看到 6 行，和之前快照读的 5 行不一致，表现为"幻读"。

结论：**RR 靠 MVCC 挡住快照读的幻读，靠 Next-Key Lock 挡住当前读的幻读，但快照读与当前读混用的缝隙仍在。** 这也是为什么说"RR 基本解决幻读"而非"彻底解决"。作业任务里会让你亲手复现这个缝隙。

---

## 八、事务用在哪一层：Spring 入口预告

裸 SQL 看清机制后，工程里事务的入口是 Spring 的 `@Transactional`：

```java
@Service
public class OrderService {
    // @Transactional 底层就是帮你 BEGIN / COMMIT / 出异常 ROLLBACK
    @Transactional
    public void placeOrder(Long skuId, int qty) {
        int affected = skuMapper.deductStock(skuId, qty);  // UPDATE ... WHERE stock >= qty（当前读）
        if (affected == 0) {
            throw new BusinessException("库存不足");        // 抛异常 → 整个事务回滚
        }
        orderMapper.insert(...);
    }
}
```

几个"现在先知道、第 08 章细讲"的点：

- 事务的**传播行为**（`REQUIRED`/`REQUIRES_NEW` 等）、**回滚规则**（默认只对运行时异常回滚）、隔离级别覆盖，都能在 `@Transactional` 上配。
- **事务方法要尽量短**：事务期间持有的锁和连接都不释放，长事务会拖垮连接池（M02-07）、放大锁冲突（M02-06）。
- **别在事务里做远程调用 / 发消息 / 大循环**——那是把外部延迟绑进了锁的持有时间。

> 本章不展开 Spring，只建立一个认知：**你在裸 SQL 看到的隔离级别、当前读、锁，最终都由 `@Transactional` 包着的方法在承受。** 机制不清楚，框架用起来就是黑盒。

---

## 九、常见坑与误区 ⚠️

| 坑 / 误区 | 真相 |
|---|---|
| ACID 里一致性是一个独立机制 | C 是**目的**，靠 A（undo）+ I（锁/MVCC）+ D（redo）+ 约束共同达成 |
| 原子性靠 redo log | 反了：**原子性/回滚靠 undo log**，持久性/恢复靠 redo log |
| MySQL 默认隔离级别是 RC | 是 **RR**（Oracle/PG 才默认 RC） |
| RR 完全杜绝幻读 | 快照读靠 MVCC、当前读靠 Next-Key Lock **基本**解决；快照读与当前读混用仍可构造幻读 |
| 不可重复读和幻读是一回事 | 不可重复读是**行的值变了**(UPDATE)，幻读是**行数变了**(INSERT/DELETE) |
| 普通 SELECT 读的一定是最新数据 | 快照读读的是 **ReadView 对应的历史版本**，可能不是此刻最新 |
| RC 和 RR 机制完全不同 | 同一套 MVCC，**只差 ReadView 生成时机**：RC 每次读新建、RR 事务内复用 |
| 先 SELECT 判断库存再 UPDATE 扣减 | 快照读的值不可信，并发会超卖；要用**当前读**（`UPDATE ... WHERE stock>0` 看影响行数） |
| `SELECT ... FOR UPDATE` 和普通查询一样 | 它是**当前读 + 加锁**，读最新值并阻塞别人修改 |
| MVCC 让所有操作都不加锁 | 只有**快照读**不加锁；**当前读和写**照样加锁 |
| 事务开着不影响性能 | 长事务持锁、占连接、undo 链变长（回滚段膨胀），是线上大坑 |

---

## 十、小结

| 知识点 | 记住这一句 |
|---|---|
| ACID | A 靠 undo、D 靠 redo、I 靠锁+MVCC，C 是这三者+约束达成的目的 |
| 三类读问题 | 脏读=读未提交；不可重复读=行值变(UPDATE)；幻读=行数变(INSERT/DELETE) |
| 四级隔离 | RU→RC→RR→SERIALIZABLE，越高越安全越慢；MySQL 默认 **RR** |
| MySQL 默认 RR | 历史上为 statement binlog 主从一致；InnoDB 强化到基本防幻读 |
| MVCC 三件套 | 隐藏列(trx_id/roll_ptr) + undo 版本链 + ReadView 可见性判断 |
| RC vs RR | 唯一区别：ReadView **RC 每次读新建、RR 事务内复用** |
| 快照读 | 普通 SELECT，走 MVCC 读历史版本，不加锁 |
| 当前读 | `FOR UPDATE`/`UPDATE`/`DELETE`，读最新版本 + 加锁 |
| 扣库存 | 用当前读：`UPDATE ... WHERE stock>0`，看影响行数，杜绝超卖 |
| RR 防幻读 | 快照读靠 MVCC、当前读靠 Next-Key Lock，混用仍有缝隙 |

---

## 十一、自检问题

不查资料，先口头回答：

1. ACID 四性分别靠 InnoDB 的什么机制实现？为什么说一致性是"目的"不是"机制"？
2. 脏读、不可重复读、幻读的定义各是什么？不可重复读和幻读的本质区别在哪（分别由什么 SQL 引起）？
3. 四种隔离级别分别允许哪些问题发生？MySQL 默认哪个？为什么不是 RC？
4. MVCC 靠哪三样东西工作？`DB_TRX_ID` 和 `DB_ROLL_PTR` 各是什么？
5. ReadView 的可见性判断规则说一遍。`trx_id` 落在 `[min_trx_id, max_trx_id)` 且在 `m_ids` 里，可见吗？
6. RC 和 RR 在 MVCC 上唯一的区别是什么？用它解释"为什么 RR 能可重复读、RC 不能"。
7. 快照读和当前读分别是什么？哪些语句是当前读？
8. 电商扣库存为什么不能"先 SELECT 判断再 UPDATE"？正确写法是什么，怎么判断扣成功？
9. RR 下幻读被解决到什么程度？举一个仍能构造出幻读的场景。

答不上 5、6、8 的，回看第五、六节——它们是本章最常被面试追问、也最直接影响业务正确性的点。

---

## 十二、延伸阅读

- [MySQL 8.4 Reference · Transaction Isolation Levels](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html)（四级隔离与 InnoDB 行为）
- [MySQL 8.4 Reference · Consistent Nonlocking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)（快照读 / MVCC）
- [MySQL 8.4 Reference · Locking Reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html)（`FOR UPDATE` / `FOR SHARE` 当前读）
- [MySQL 8.4 Reference · InnoDB Multi-Versioning](https://dev.mysql.com/doc/refman/8.4/en/innodb-multi-versioning.html)（undo、隐藏列、版本链）
- [MySQL 8.4 Reference · redo log / undo log](https://dev.mysql.com/doc/refman/8.4/en/innodb-redo-log.html)

> 版本相关行为（默认 RR、Next-Key Lock 对幻读的处理、ReadView 实现细节）以官方文档为准，本章基于 MySQL 8.4 / InnoDB。具体某个并发场景的实际表现，**最终以你在 `mysql84` 上开两个会话跑出来的结果为准**。Content was rephrased for compliance with licensing restrictions.

---

## 下一步

完成[作业 05 · 事务、隔离级别与 MVCC 实战](../作业/05-事务隔离级别与MVCC实战.md)。核心目标：

1. **观察 / 分析线（experimental）**：在 `mysql84` 上**开两个会话**，亲手复现脏读（RU）、不可重复读（RC vs RR 对比）、快照读 vs 当前读的差异，并复现一次 RR 下"快照读+当前读混用"的幻读缝隙。
2. 本章仍以**机制观察和并发正确性理解**为主，**不新增 delivered 正式代码**（事务真正落到 `product-service` 是第 08 章持久层集成 + 第 10 章落地改造的事）。

> 下一章 **M02-06 锁：行锁 · 间隙锁 · Next-Key · 死锁排查**：本章反复提到的"当前读加什么锁、间隙锁怎么防幻读、死锁怎么来的怎么查"，下一章全部展开。
