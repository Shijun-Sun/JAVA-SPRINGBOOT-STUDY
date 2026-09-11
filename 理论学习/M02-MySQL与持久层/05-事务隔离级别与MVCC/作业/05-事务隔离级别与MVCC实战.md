# 作业 05 · 事务、隔离级别与 MVCC 实战

> **对应讲义**：[M02-05 · 事务、隔离级别与 MVCC](../学习/05-事务隔离级别与MVCC.md)
> **预计耗时**：3～4 小时
> **难度**：中等偏上（并发现象要开两个会话、掐时间点复现，第一次做慢很正常）
> **Review 模式**：默认"只指出错误，不直接给答案"

> **本章作业性质说明（重要）**
> 本章是**并发正确性的机制观察训练**，几乎全部是**知识点实验 / 观察**，落 `homework/experimental/`：
> - 复用 03/04 章已经起好的 Docker MySQL：容器 `mysql84`、库 `mall`、表 `spu`/`sku`/`category` 已建好。
> - 本章**不新增 delivered 正式代码**。事务真正落到 `product-service` 是第 08 章（持久层集成 `@Transactional`）+ 第 10 章（落地改造）的事；本章先把"隔离级别、MVCC、快照读 vs 当前读"在裸 SQL 层看透。
> - 核心手段是**开两个 MySQL 会话（Session A / Session B）**，按讲义的时间轴交替执行，观察对方未提交/已提交的改动对自己可见性的影响。

---

## 一、前置：准备两个会话 + 一行可观察数据

### 1.1 开两个独立会话

并发现象必须两个会话交替执行。任选一种方式，**开两个终端窗口**分别连进去：

```bash
# 会话 A（终端 1）
docker exec -it mysql84 mysql -uroot -proot123456 mall
# 会话 B（终端 2）——另开一个终端，重复同样命令
docker exec -it mysql84 mysql -uroot -proot123456 mall
```

> 也可以用 IDEA Database 面板 / DBeaver 各开一个 console —— 但要确认两个 console 是**不同的连接（不同 session）**，否则观察不到并发效果。命令行最直观，推荐。

### 1.2 准备一行观察数据

```sql
-- 任一会话执行一次即可，准备一行确定的 sku
INSERT INTO sku (id, spu_id, sku_code, price, stock, status)
VALUES (100, 1, 'OBS-SKU-100', 99.00, 10, 1)
ON DUPLICATE KEY UPDATE stock = 10;   -- 重跑实验时把库存重置回 10
```

每个实验开始前，先把这行 `stock` 复位成 10，保证现象可复现。

### 1.3 每题都要记录的东西

> 落位：`homework/experimental/txn-isolation-lab/`（自建目录），建议每个实验一个 `.md`，贴**两个会话的执行顺序 + 每步的真实输出**，最后写一句结论。
> - 性质：知识点实验（并发机制观察）
> - 实现位置：`homework/experimental/txn-isolation-lab/`
> - 前置交付：03 章已建 `sku` 表（delivered schema）；无需新增 delivered
> - 是否阻塞推进：否（但这是进第 06 章"锁"的**理解门槛**）

---

## 二、观察线（必做，落 experimental）

> 统一约定：下表 `A>` 表示在会话 A 执行、`B>` 表示在会话 B 执行，**从上往下按顺序**交替敲。

### 任务一：复现脏读（READ UNCOMMITTED）

> 级别：必做 / 阻塞：否 / 验证：输出真实、能指出"脏"在哪

两个会话都设成读未提交：

```sql
-- A 和 B 都先执行
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
```

按顺序执行并记录每一步 A 读到的值：

```sql
A> BEGIN;
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：应为 10
B> BEGIN;
B> UPDATE sku SET stock = 0 WHERE id = 100;       -- 注意：B 不提交！
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：读到几？
B> ROLLBACK;                                       -- B 反悔
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：又变回几？
A> COMMIT;
```

**验收**：说明 A 第二次为什么读到 0（脏读：读到 B 未提交的值）、B 回滚后又变回 10，指出"基于脏读做判断"的危害。

### 任务二：RC 下的不可重复读

> 级别：必做 / 阻塞：否

```sql
-- A 和 B 都设成读已提交
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

```sql
A> BEGIN;
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：10
B> UPDATE sku SET stock = 5 WHERE id = 100;
B> COMMIT;                                          -- 这次 B 提交了
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：读到几？
A> COMMIT;
```

**验收**：A 两次读同一行值不同（10 → 5），说明这就是"不可重复读"，并联系讲义 5.4——RC 每次 SELECT 新建 ReadView，所以看得到 B 已提交的新值。

### 任务三：RR 防住不可重复读（和任务二对照）⭐

> 级别：必做 / 阻塞：否

先把 `stock` 复位成 10，两个会话都设成 RR：

```sql
UPDATE sku SET stock = 10 WHERE id = 100;   -- 复位
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;   -- A、B 都设
```

```sql
A> BEGIN;
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：10（第一次快照读，建 ReadView）
B> UPDATE sku SET stock = 5 WHERE id = 100;
B> COMMIT;
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：读到几？（关键！）
A> COMMIT;
A> SELECT stock FROM sku WHERE id = 100;          -- 记录：事务结束后再读，几？
```

**验收**：
- A 在事务内两次读都是 **10**（复用第一次的 ReadView，看不到 B 的提交）；
- A 提交后再读变成 **5**；
- 用讲义 5.4 的"RR 事务内复用 ReadView"解释为什么 RR 能可重复读，而任务二的 RC 不能。**这是本章最能体现 MVCC 的对照实验。**

### 任务四：快照读 vs 当前读（RR 下）⭐⭐

> 级别：必做 / 阻塞：否

复位 `stock=10`，两会话 RR。这题揭示"同一事务里，普通 SELECT 和 FOR UPDATE 读到的值可能不一样"：

```sql
A> BEGIN;
A> SELECT stock FROM sku WHERE id = 100;               -- 快照读，记录：10
B> UPDATE sku SET stock = 8 WHERE id = 100;
B> COMMIT;
A> SELECT stock FROM sku WHERE id = 100;               -- 快照读，记录：?（仍是快照）
A> SELECT stock FROM sku WHERE id = 100 FOR UPDATE;    -- 当前读，记录：?（读最新）
A> COMMIT;
```

**验收**：
- 两次普通 `SELECT` 都读到 **10**（快照读，走 MVCC 老快照）；
- `FOR UPDATE` 读到 **8**（当前读，读最新已提交值）；
- 说明"快照读读历史版本、当前读读最新版本 + 加锁"，并指出：**这正是电商不能用快照读判断库存的原因**（联系讲义 6.3）。

### 任务五：当前读会互相阻塞（行锁初体验）

> 级别：必做 / 阻塞：否（为第 06 章锁铺垫）

复位 `stock=10`，两会话 RR：

```sql
A> BEGIN;
A> SELECT stock FROM sku WHERE id = 100 FOR UPDATE;    -- A 拿到行的排他锁
B> UPDATE sku SET stock = stock - 1 WHERE id = 100;    -- 观察：B 会怎样？
```

**验收**：记录 B 的现象（B 被**阻塞等待**，直到 A `COMMIT`/`ROLLBACK` 或超时报 `Lock wait timeout`）。A 提交后再看 B 是否继续。说明"当前读加锁 → 别的当前读/写要等锁释放"，这就是第 06 章行锁的入口。做完记得 `A> COMMIT;` 释放锁。

---

## 三、验证扣库存的正确写法（必做，落 experimental）⭐

### 任务六：错误写法 vs 正确写法的并发对比

> 级别：必做 / 阻塞：否 / 验证：能说清为什么正确写法不超卖

复位 `stock=1`（只剩一件，制造抢购）。

**A. 先演示错误思路**（不要求真的并发出 -1，只要说清风险）：

```sql
-- 错误：快照读判断 + 分开的 UPDATE，并发两个事务都可能读到 stock=1 都扣
A> BEGIN; SELECT stock FROM sku WHERE id=100;   -- 读到 1
B> BEGIN; SELECT stock FROM sku WHERE id=100;   -- 也读到 1
A> UPDATE sku SET stock = stock - 1 WHERE id=100; A> COMMIT;
B> UPDATE sku SET stock = stock - 1 WHERE id=100; B> COMMIT;   -- stock 变成 -1
B> SELECT stock FROM sku WHERE id=100;          -- 记录：超卖成 -1
```

**B. 正确写法**（复位 `stock=1`）：

```sql
A> BEGIN;
A> UPDATE sku SET stock = stock - 1 WHERE id = 100 AND stock > 0;   -- 当前读 + 条件
-- 记录 affected rows（应为 1）
B> UPDATE sku SET stock = stock - 1 WHERE id = 100 AND stock > 0;   -- B 阻塞等 A
A> COMMIT;
-- A 提交后 B 继续：记录 B 的 affected rows（应为 0，stock 已是 0，条件不满足）
B> COMMIT;
B> SELECT stock FROM sku WHERE id = 100;         -- 记录：0，没超卖
```

**验收**：对比 A/B 两种写法的最终 `stock`，说明为什么正确写法：
1. `UPDATE` 是当前读，B 会等 A 的行锁；
2. `AND stock > 0` 让第二个扣减 `affected rows = 0`，应用层据此返回"库存不足"；
3. 一句话总结："判断+扣减"必须在**一条原子的条件 UPDATE** 里，看影响行数决定成败。

---

## 四、进阶 / 挑战（选做，只需分析或复现）

### 任务七：复现 RR 下"快照读 + 当前读混用"的幻读缝隙（进阶）⭐

> 级别：进阶 / 阻塞：否

复位数据，两会话 RR。用一个范围查询演示讲义第七节的边角幻读：

```sql
A> BEGIN;
A> SELECT COUNT(*) FROM sku WHERE spu_id = 1;              -- 快照读，记录行数 N
B> INSERT INTO sku (spu_id, sku_code, price, stock, status)
   VALUES (1, 'PHANTOM-1', 1.00, 1, 1);
B> COMMIT;
A> SELECT COUNT(*) FROM sku WHERE spu_id = 1;              -- 快照读，记录：还是 N（MVCC 挡住）
A> SELECT COUNT(*) FROM sku WHERE spu_id = 1 FOR UPDATE;   -- 当前读，记录：N+1（看到幻影行）
A> COMMIT;
```

**验收**：说明为什么两次快照读都是 N（MVCC 复用快照）、而当前读变成 N+1（读最新），这就是"快照读与当前读混用"的幻读缝隙。清理：删掉 `PHANTOM-1` 那行。

### 任务八：隔离级别与现象对照表（纯分析）

> 落位：`homework/experimental/txn-isolation-lab/隔离级别分析.md`

不查表，凭理解填一张矩阵，再回讲义第四节核对：

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 你实验里对应的任务 |
|---------|------|-----------|------|------------------|
| READ UNCOMMITTED | ? | ? | ? | 任务一 |
| READ COMMITTED | ? | ? | ? | 任务二 |
| REPEATABLE READ | ? | ? | ? | 任务三、四、七 |
| SERIALIZABLE | ? | ? | ? | — |

**验收**：矩阵填对，并用一句话解释"MVCC 的 ReadView 生成时机（RC 每次 / RR 一次）如何决定了 RC 与 RR 那一行的差异"。

### 任务九：MVCC 可见性判断（挑战，面试级）

给定版本链和 ReadView，判断当前事务读到哪个版本（纯推理，写出判断依据）：

- 某行版本链：`v3(trx_id=50, stock=5) → v2(trx_id=30, stock=8) → v1(trx_id=10, stock=10)`
- 当前事务的 ReadView：`m_ids=[30,50]`、`min_trx_id=30`、`max_trx_id=51`、`creator_trx_id=50`

问：这个事务的快照读会读到 `stock` 等于几？逐个版本套讲义 5.3 的四条规则写出判断过程。

---

## 五、提交检查清单

### 知识点实验（experimental/txn-isolation-lab/）

- [ ] 任务一：脏读复现（RU）+ 输出 + 结论
- [ ] 任务二：RC 不可重复读复现
- [ ] 任务三：RR 防不可重复读（与任务二对照）⭐
- [ ] 任务四：快照读 vs 当前读（同事务两个值不同）⭐
- [ ] 任务五：当前读互相阻塞（行锁初体验）
- [ ] 任务六：扣库存错误写法 vs 正确写法并发对比 ⭐
- [ ]（进阶）任务七 幻读缝隙 / 任务八 对照矩阵 / 任务九 MVCC 可见性推理

> 每个并发实验都要贴**两个会话的执行顺序 + 每步真实输出**，不能只写结论。做完清理实验数据（删掉 PHANTOM 行、复位 stock）。

---

## 六、交付说明（完成后填写）

```markdown
完成时间：
环境：mysql84 容器 / mall 库 / sku id=100 观察行
完成任务：观察 1-5；扣库存 6；进阶 7-9：做 / 未做

### 观察线
- 脏读(RU)：A 第二次读到 0，B 回滚后回 10 —— 输出见……
- RC 不可重复读：10 → 5
- RR 防不可重复读：事务内两次都 10，提交后 5 —— 对照 RC 的解释……
- 快照读 vs 当前读：普通 SELECT=10 / FOR UPDATE=8
- 当前读阻塞：B 被阻塞，A 提交后 B 继续 / 或 Lock wait timeout

### 扣库存
- 错误写法最终 stock=-1（超卖）；正确写法：A affected=1、B affected=0，最终 stock=0

### 进阶
- 幻读缝隙：快照读 N / 当前读 N+1……
- MVCC 可见性推理：读到 stock=__，判断过程……

遇到的问题：
尚未完成 / 需 review 的部分：
```

---

## 七、提交后如何请求 Review

完成后直接说：

> Review 作业 05（M02），只指出错误，不直接给答案。

我会按固定格式检查：结论、错误清单（定位到你记错的可见性判断或写错的实验顺序）、改进建议、架构视角（高并发下隔离级别选择、长事务对锁与连接池的影响、扣库存方案的演进）、可选优化、下一步。

> 本章 Review 会特别盯：**RC/RR 的 ReadView 时机说不说得清、快照读 vs 当前读分不分得开、扣库存为什么必须当前读**——这三点是"能不能写出并发安全的业务代码"的分水岭，也是第 06 章锁和后续下单/库存设计的地基。
