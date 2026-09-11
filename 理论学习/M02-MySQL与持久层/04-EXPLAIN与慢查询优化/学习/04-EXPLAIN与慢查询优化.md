# M02-04 · EXPLAIN 与慢查询优化：让"快不快"从猜测变成可验证

> **预计阅读时间**：130～160 分钟
> **配套作业**：[作业 04 · EXPLAIN 与慢查询优化实战](../作业/04-EXPLAIN与慢查询优化实战.md)
> **版本基线**：MySQL 8.4 LTS、InnoDB、默认页 16KB、`utf8mb4`
> **前置**：[M02-02 · InnoDB 存储结构与 B+ 树](../../02-InnoDB存储结构与B+树/学习/02-InnoDB存储结构与B+树.md)、[M02-03 · 索引设计与最左前缀原则](../../03-索引设计与最左前缀原则/学习/03-索引设计与最左前缀原则.md)（本章全程用到回表、覆盖、最左前缀）

> **本章目标**
> - 会用 `EXPLAIN` 的三种形态（估算 / JSON / `ANALYZE` 真实执行），知道各自看什么
> - **逐列读懂** `EXPLAIN` 输出：重点吃透 `type`、`key` / `key_len`、`rows` / `filtered`、`Extra`
> - 会算 `key_len`，用它反推"联合索引到底用到了第几列"
> - 会开**慢查询日志**、设 `long_query_time`、用工具聚合慢 SQL，找出"该优化哪一条"
> - 掌握一套**从慢 SQL 到优化**的固定流程（SOP），能对一条真实慢查询做"优化前后对比"
> - 吃透几个高频优化案例：回表多→覆盖、`filesort`→索引排序、**深分页**、隐式转换、统计信息过期

---

## 一、我们在解决什么问题：索引设计完，怎么验收？

03 章你给商品域设计了索引（`idx_parent_sort`、`idx_category_status_created`、`idx_spu_id`）。但"设计得对不对、SQL 到底走没走上索引、走了之后要扫多少行、有没有回表、有没有额外排序"——这些**光看 SQL 猜不出来**，必须问 MySQL 自己。

`EXPLAIN` 就是问 MySQL：**"这条 SQL 你打算怎么执行？"** 它把优化器的执行计划摊开给你看：用哪个索引、访问方式是什么、预计扫多少行、要不要回表、要不要额外排序。**索引设计的效果，最终要靠执行计划来验收；线上慢 SQL 的诊断，第一步永远是 `EXPLAIN`。**

本章分三段：

1. **读懂 `EXPLAIN`**（第二~五节）：逐列解释，重点是 `type`、`key_len`、`Extra`。
2. **找到慢 SQL**（第六节）：慢查询日志怎么开、怎么聚合，别等用户投诉才知道慢。
3. **优化并验证**（第七~八节）：一套 SOP + 几个真实案例，每个都有"改之前 `EXPLAIN` 什么样、改之后什么样"。

> 一句话定位：**03 章是"设计索引"，04 章是"用执行计划验收索引、并诊断和优化慢 SQL"。** 这是从"会建索引"到"会调优"的关键一步，也是面试问得最细的一块。

---

## 二、EXPLAIN 的三种形态：估算、JSON、真实执行 ⭐

`EXPLAIN` 放在 `SELECT`（也可 `UPDATE`/`DELETE`/`INSERT ... SELECT`）前面：

```sql
EXPLAIN SELECT id, name FROM spu WHERE category_id = 3 AND status = 1 ORDER BY created_at DESC;
```

**关键认知：普通 `EXPLAIN` 不真正执行查询**，它只让优化器给出"打算怎么走"的计划，里面的 `rows`、`filtered` 都是**基于统计信息的估算值**，不是真实数字。

三种形态，用途不同：

| 形态 | 命令 | 会不会真跑 | 看什么 |
|------|------|-----------|--------|
| **传统表格** | `EXPLAIN <sql>` | 否（估算） | 日常首选，快速看 `type`/`key`/`rows`/`Extra` |
| **JSON 详情** | `EXPLAIN FORMAT=JSON <sql>` | 否（估算） | 看优化器**成本**（`cost`）、更细的过滤链，做深度分析 |
| **真实执行** | `EXPLAIN ANALYZE <sql>` | **是（真跑）** | 看**实际**耗时、**实际**行数 vs 估算行数，抓"估算严重失真"的问题 |

> ⚠️ `EXPLAIN ANALYZE`（MySQL 8.0.18+）会**真正执行** SQL。对 `SELECT` 一般安全；但对 `UPDATE`/`DELETE` 会真的改数据，**别在生产库对写语句用它**。

`EXPLAIN ANALYZE` 的价值：普通 `EXPLAIN` 说"预计扫 100 行"，但统计信息过期时可能实际扫了 100 万行。`ANALYZE` 把**估算值和真实值并排给你**，一眼看出优化器是不是被"骗"了（第七节讲统计信息）。

---

## 三、逐列读懂传统 EXPLAIN ⭐⭐

一条 `EXPLAIN` 表格输出有这些列（MySQL 8.4）：

```
id | select_type | table | partitions | type | possible_keys | key | key_len | ref | rows | filtered | Extra
```

按"最该看的顺序"讲，而不是从左到右。

### 3.1 `type`：访问类型 —— 最重要的一列 ⭐⭐

`type` 表示 MySQL 找到目标行的**方式**，直接反映快慢。从好到坏：

| type | 含义 | 好坏 |
|------|------|------|
| `system` | 表只有一行（系统表） | 最好（极少见） |
| `const` | 主键 / 唯一索引 **等值**匹配，最多返回一行 | 极好 |
| `eq_ref` | JOIN 时，对每一行用主键/唯一键在另一表**精确匹配一行** | 很好 |
| `ref` | 普通索引 **等值**匹配，可能返回多行 | 好（索引等值查询的常态） |
| `range` | 索引**范围**扫描（`>`、`<`、`BETWEEN`、`IN`、`LIKE 'x%'`） | 还行 |
| `index` | **扫描整棵索引树**（全索引扫，比全表略好，仍是扫描） | 偏差 ⚠️ |
| `ALL` | **全表扫描**，逐行读聚簇索引 | 最差 ⚠️⚠️ |

**实战判断线**：
- 看到 `const` / `eq_ref` / `ref` / `range`：走上索引了，一般 OK。
- 看到 **`ALL`**：全表扫描，数据量一大就是慢 SQL，**重点排查对象**。
- 看到 **`index`**：别被名字骗了——它是"扫完整个索引"，不是"高效走索引"。常见于"用索引避免了排序但没有好的过滤条件"，也要警惕。

> ⚠️ 小表（几十行）出现 `ALL` 很正常，优化器算出"全表扫比走索引还快"就直接扫。`type` 要结合 `rows`（扫多少行）一起看，不是看到 `ALL` 就一定有问题。

### 3.2 `key` / `possible_keys` / `key_len`：用了哪个索引、用到多深 ⭐

- **`possible_keys`**：这条查询**可能**用到的索引（候选）。为 `NULL` 说明没有可用索引，通常要建索引。
- **`key`**：优化器**实际选用**的索引。为 `NULL` = 没走索引（全表扫）。
- **`key_len`**：用到了索引的**前多少字节**——这是**反推"联合索引用到了第几列"的关键**。

#### key_len 怎么算 ⭐

`key_len` = 用到的各列在索引里占用的字节数之和。每列的字节数由**类型 + 是否可空 + 字符集**决定：

| 列情况 | 额外字节 |
|--------|---------|
| 列**可为 NULL** | +1 字节（存"是不是 NULL"的标记） |
| **变长类型**（`VARCHAR`/`VARBINARY`） | +2 字节（存实际长度） |

常见类型的基础字节：

| 类型 | 字节 |
|------|------|
| `TINYINT` | 1 |
| `INT` | 4 |
| `BIGINT` | 8 |
| `DATETIME`（8.0） | 5 |
| `CHAR(n)` utf8mb4 | `3n` 或 `4n`（按字符集，utf8mb4 每字符最多 4 字节） |
| `VARCHAR(n)` utf8mb4 | `4n + 2`（变长再 +2） |

**例**：`spu` 上有 `idx_category_status_created (category_id BIGINT UNSIGNED NOT NULL, status TINYINT NOT NULL, created_at DATETIME NOT NULL)`。

- 只用到 `category_id`：`key_len = 8`
- 用到 `category_id + status`：`key_len = 8 + 1 = 9`
- 用到 `category_id + status + created_at`：`key_len = 8 + 1 + 5 = 14`

所以看到 `key_len = 9`，就知道**只用到了前两列**（`created_at` 没用上定位）。这比肉眼猜"最左前缀走到第几列"精确得多。

> 一句话：**`key_len` 是"最左前缀走到第几列"的度量尺。** 联合索引调优必看这一列。

### 3.3 `rows` / `filtered`：预计要扫多少、过滤后剩多少 ⭐

- **`rows`**：优化器**估算**这一步要扫描的行数。**越小越好**。它是估算值（基于统计信息），不是精确值。
- **`filtered`**：扫描后，**估算**有百分之多少的行满足 `WHERE` 的其余条件（0~100）。

"预计交给下一步/返回的行数" ≈ `rows × filtered%`。

- `rows` 很大（几十万、上百万）+ `type=ALL` → 典型慢 SQL。
- `filtered` 很低（比如 1%）却还 `type=ALL` → 扫了一大堆最后只留一点点，索引没建对或没建。

### 3.4 `Extra`：最能暴露问题的附加信息 ⭐⭐

`Extra` 是文字说明，常见值（会同时出现多个）：

| Extra | 含义 | 信号 |
|-------|------|------|
| **`Using index`** | **覆盖索引**，不回表 | 好 ✅ |
| **`Using index condition`** | **ICP 索引下推**，在索引层先过滤，减少回表 | 较好 |
| `Using where` | 存储引擎返回后，Server 层还要再过滤 | 中性，常见 |
| **`Using filesort`** | 需要**额外排序**（`ORDER BY` 没走上索引） | 警惕 ⚠️ |
| **`Using temporary`** | 用了**临时表**（常见于 `GROUP BY`/`DISTINCT`/复杂 `ORDER BY`） | 警惕 ⚠️ |
| `Using join buffer` | JOIN 没走索引，用了连接缓冲（BNL/hash join） | 警惕 ⚠️ |
| `Select tables optimized away` | 聚合被优化掉（如 `MAX()` 直接读索引边界） | 好 |
| `Impossible WHERE` | 条件恒假，不用查 | — |

**两个最该盯的**：
- **`Using filesort`**：不是"排序到文件"的意思（可能在内存排），而是"**没能利用索引的有序性、要额外做一次排序**"。大结果集的 filesort 很贵，是优化重点。
- **`Using temporary`**：建临时表存中间结果，`GROUP BY` 和无法用索引的去重/排序常触发，代价高。

> `Using index`（覆盖，不回表）和 `Using index condition`（ICP，减少回表但仍回表）别混——03 章反复强调过。

### 3.5 `id` / `select_type` / `table` / `ref`：多表和子查询才重点看

- **`id`**：执行计划里每个 `SELECT` 的编号。`id` **越大越先执行**；`id` 相同则**从上到下**执行。多表 JOIN、子查询时用它看执行顺序。
- **`select_type`**：查询类型。`SIMPLE`（简单查询）、`PRIMARY`（最外层）、`SUBQUERY`（子查询）、`DERIVED`（派生表/子查询当表用）、`UNION` 等。
- **`table`**：这一行对应哪张表（或派生表 `<derivedN>`）。
- **`ref`**：走索引等值匹配时，索引列**和谁比**——常量 `const`，或另一张表的某列（JOIN 时）。

单表查询这几列信息量不大；到多表 JOIN、子查询优化时它们才关键。

---

## 四、动手读一遍：拿我们真实的表来看

前提：M02 已经用 Docker 起了 `mysql:8.4`（容器 `mysql84`，库 `mall`，已建好 `category`/`spu`/`sku`）。**空表看不出效果，得先灌一些数据**（作业里给了灌数脚本，这里先示意读法）。

```sql
-- 走上联合索引、且用索引完成排序的理想情况
EXPLAIN SELECT id, name
FROM spu
WHERE category_id = 3 AND status = 1
ORDER BY created_at DESC;
```

期望读到：
- `type = ref`（普通索引等值） → 走上索引了；
- `key = idx_category_status_created`；
- `key_len = 9`（用到 `category_id` + `status` 两个等值列）；
- `Extra` **没有** `Using filesort`（`created_at` 在该段有序，`ORDER BY ... DESC` 顺着索引反向读）。

对照一个"最左前缀断裂"的反例：

```sql
-- 缺最左列 category_id，取整行
EXPLAIN SELECT * FROM spu WHERE status = 1;
```

期望读到：`key = NULL`、`type = ALL`（用不上联合索引，全表扫）。

> ⚠️ **一个极易踩的坑（本章作者在真实数据上验证过）**：如果把上面写成
> `EXPLAIN SELECT id FROM spu WHERE status = 1`，`type` 不是 `ALL` 而是 **`index`**、`Extra` 还有 `Using index`！
> 为什么？因为 `SELECT id` 要的列（`id` 是主键）以及条件列 `status` **都在联合索引的叶子里**——查询被这个索引**覆盖**了。优化器发现"扫二级索引比扫聚簇索引（整行更宽）更省"，于是做**全索引扫描**（`type=index`）而不是全表扫描（`type=ALL`）。
> 这恰恰说明：**`type` 会被 `SELECT` 的列影响**。要观察"缺最左列 → 全表扫"，得用 `SELECT *`（强制回表取整行、覆盖不了）。这也是"能覆盖就尽量覆盖"的又一例证。

> 本章作业的观察线，会让你在真实 MySQL 上把这些 `EXPLAIN` 亲手跑一遍、贴出输出——**读执行计划这件事，看十遍不如自己跑一遍**。上面这些"期望值"都是在 10 万行 `spu` 上实测出来的，你的环境数据分布不同，`rows`/`filtered` 会有差异，但 `type`/`key`/`Extra` 的结论应当一致。

---

## 五、EXPLAIN ANALYZE：估算 vs 真实 ⭐

```sql
EXPLAIN ANALYZE
SELECT id, name FROM spu WHERE category_id = 3 AND status = 1 ORDER BY created_at DESC;
```

输出是一棵**树**（从最内层往外读），每个节点带两组数字：

```
-> Index lookup on spu using idx_category_status_created ...
   (cost=... rows=120)                    ← 优化器估算
   (actual time=0.03..0.11 rows=118 loops=1)  ← 真实执行
```

- `cost` / `rows`：**估算**（和普通 EXPLAIN 一致）。
- `actual time` / `rows` / `loops`：**真实**测出来的。
  - `actual time=A..B`：A = 返回**第一行**耗时，B = 返回**最后一行**耗时（毫秒）。
  - `rows`：这一步**实际**产出的行数。
  - `loops`：这一步被执行了几次（JOIN 内层会被外层驱动多次）。

**怎么用**：把 `rows`（估算）和 `actual ... rows`（真实）对比。
- 两者接近 → 优化器判断准，计划可信。
- **差好几个数量级**（估算 100、实际 100 万）→ **统计信息过期**，优化器在错误的数据上选了错误的计划，该 `ANALYZE TABLE` 更新统计信息（第七节）。

---

## 六、慢查询日志：先找到"该优化哪一条" ⭐

优化的前提是**知道哪条慢**。不能等用户投诉，要让数据库自己把慢 SQL 记下来。

### 6.1 关键参数

```sql
-- 看当前配置
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';

-- 运行期打开（重启失效；永久生效要写 my.cnf / 启动参数）
SET GLOBAL slow_query_log = ON;             -- 开启慢日志
SET GLOBAL long_query_time = 1;             -- 超过 1 秒算慢（可设 0.5 等小数）
SET GLOBAL log_queries_not_using_indexes = ON;  -- 没走索引的也记（调优期开，平时慎开）
```

| 参数 | 作用 | 注意 |
|------|------|------|
| `slow_query_log` | 总开关 | `SET GLOBAL` 改的重启丢失 |
| `long_query_time` | 慢阈值（秒），支持小数 | 默认 10s，太大；调优期常设 1s 或更低 |
| `slow_query_log_file` | 日志文件路径 | 容器里在数据目录下 |
| `log_queries_not_using_indexes` | 记录未走索引的语句 | 平时开会刷爆日志，**只在排查时开** |
| `log_output` | `FILE`（默认）/ `TABLE`（写进 `mysql.slow_log`） | `TABLE` 便于 SQL 查询分析 |

> `SET GLOBAL` 只对**新连接**生效，且重启丢失。要永久生效写进配置文件（容器可用挂载的 `my.cnf` 或 `command` 启动参数），这在第 10 章落地时统一配。

### 6.2 慢日志怎么看：别逐行读，要聚合

慢日志一条条读没意义，要**按"SQL 模板"聚合**，找出"最该优化的 Top N"：

- **`mysqldumpslow`**（MySQL 自带）：按次数/耗时排序聚合。
  ```bash
  mysqldumpslow -s t -t 10 /path/slow.log   # 按总耗时(t)排序，取前 10
  ```
- **`pt-query-digest`**（Percona Toolkit，更强）：生成详细报告，含耗时分布、占比。生产运维几乎标配。

聚合的意义：一条查询单次 0.5s 不算啥，但一天调用 100 万次，它就是头号瓶颈。**优化要按"总耗时 = 单次耗时 × 调用次数"排序，先啃占比最大的。**

---

## 七、从慢 SQL 到优化：一套固定流程（SOP）⭐⭐

拿到一条慢 SQL，别急着加索引。按这个顺序走：

```
1. 复现 + 量化    → 真的慢吗？慢多少？（打时间 / 慢日志）
2. EXPLAIN 看计划 → type=ALL? key=NULL? rows 巨大? Extra 有 filesort/temporary?
3. 定位根因       → 没索引 / 索引没命中(最左前缀、失效) / 回表太多 / 排序 / 深分页 / 统计过期
4. 针对性改       → 建/改索引、改写 SQL、覆盖索引、消除 filesort、改分页方式
5. EXPLAIN 复验   → type 变好? key 命中? rows 变小? filesort 消失?
6. 实测对比       → 真实耗时前后对比（EXPLAIN ANALYZE / 计时）
7. 评估副作用     → 新索引对写入的影响、索引数量、空间
```

**核心原则**：每次优化都要有**"改之前 vs 改之后"的执行计划和耗时对比**，不能凭感觉说"应该快了"。下面是几个必会的案例。

### 7.1 案例：全表扫 → 建索引（`type=ALL` → `ref`）

```sql
-- 慢：category_id 没索引，type=ALL，rows=全表
SELECT id, name FROM spu WHERE category_id = 3;
```
根因：无可用索引。改：建（或复用）以 `category_id` 打头的索引 → `type` 变 `ref`、`rows` 骤降。（03 章的 `idx_category_status_created` 最左列就是 `category_id`，天然能服务它，**不用另建**——最左前缀复用。）

### 7.2 案例：回表太多 → 覆盖索引（消除回表）

```sql
-- 走了索引但要回表取 name，回表次数 = 命中行数
SELECT id, name FROM spu WHERE category_id = 3 AND status = 1;
```
若这是超高频查询、命中行多，回表成本高（`Extra` 无 `Using index`）。改：把 `name` 纳入索引做覆盖 `(category_id, status, created_at, name)` 或专门的覆盖索引 → `Extra` 出现 `Using index`。**代价**：索引变宽、写入变重（03 章 5.1 的权衡），只对高频小查询做。

### 7.3 案例：`Using filesort` → 让排序走索引

```sql
-- Extra 出现 Using filesort：跳过了 status 直接按 created_at 排
SELECT id FROM spu WHERE category_id = 3 ORDER BY created_at DESC;
```
根因：`idx_category_status_created` 中间隔了 `status`，`created_at` 在"只固定 category_id"时跨不同 status 是乱的，排序用不上索引。
改法二选一：
- 查询补上 `status`（`WHERE category_id=3 AND status=1 ORDER BY created_at DESC`）→ 走索引排序；
- 或按真实查询需要，设计 `(category_id, created_at)` 索引。
复验：`Extra` 的 `Using filesort` 消失。

### 7.4 案例：深分页（`LIMIT 1000000, 10`）⭐⭐ 面试高频

```sql
-- 慢：要先扫描并跳过前 100 万行，再取 10 行
SELECT * FROM spu ORDER BY id LIMIT 1000000, 10;
```
根因：`LIMIT m, n` 会**扫描 m+n 行再丢弃前 m 行**。偏移越大越慢，且每行都回表。

改法一 **延迟关联（覆盖索引定位主键，再回表）**：
```sql
SELECT s.* FROM spu s
JOIN (SELECT id FROM spu ORDER BY id LIMIT 1000000, 10) t ON s.id = t.id;
```
子查询只在索引上扫 `id`（覆盖，不回表），回表只发生在最后 10 行。

改法二 **游标 / 键集分页（记住上一页最后一个 id）**，最优但要求按 id 顺序翻页：
```sql
SELECT * FROM spu WHERE id > 上一页最后一个id ORDER BY id LIMIT 10;
```
直接用索引定位到断点，**不扫被跳过的行**，翻到多深都稳定快。代价：不能随机跳页（只能上一页/下一页）。

> 深分页是电商列表、后台导出的经典坑。**记住：`LIMIT` 偏移量大 = 扫了一大堆再扔掉。** 解法是"用索引跳过"而不是"扫过去再丢弃"。

### 7.5 案例：隐式转换 / 函数导致失效（03 章 7.1~7.2）

`WHERE sku_code = 202609`（列是 `VARCHAR`）、`WHERE DATE(created_at) = '...'` 这类，`EXPLAIN` 会显示 `key=NULL`、`type=ALL`。改成类型一致、裸列范围即可。用 `EXPLAIN` 能直接验证"改写后 `key` 命中了"。

### 7.6 案例：统计信息过期 → `ANALYZE TABLE`

优化器靠**统计信息**（各索引的基数 cardinality）估算 `rows` 来选计划。大量增删改后统计信息可能失真，导致选错索引甚至弃用索引。

```sql
SHOW INDEX FROM spu;      -- 看 Cardinality（基数）是否明显不合理
ANALYZE TABLE spu;        -- 重新采样统计信息
```
用 `EXPLAIN ANALYZE` 对比"估算 rows vs 真实 rows"，差距大就 `ANALYZE TABLE`。这是"SQL 没改、索引没动，却突然变慢"的常见元凶之一。

---

## 八、优化的"全局观"：索引不是唯一解 ⭐

加索引是最常用的手段，但不是每次都靠它。架构视角要同时考虑：

- **SQL 本身**：`SELECT *` 改成只取需要的列（利于覆盖、减少网络与回表）；能用 `EXISTS` 别用 `COUNT`；避免无意义的 `ORDER BY`。
- **数据量层面**：单表过大（千万级以上）时，光靠索引也压不住，要考虑**归档冷数据、分区、分库分表**（M08/M12）。
- **访问层面**：热点查询用**缓存**挡在 DB 前面（M03 Redis），别让每次都打到库。
- **写读比**：写多的表少建索引（每个索引都拖慢写入）；读多写少的表可以多几个针对性索引/覆盖索引。
- **连接数**：慢 SQL 会长时间占用连接，拖垮连接池（M02-07 HikariCP），优化慢 SQL 本身也是在保护连接池。

> 一句话：**`EXPLAIN` 解决"这条 SQL 怎么走得快"，架构解决"要不要让这条 SQL 打到库、库该不该扛这么大数据量"。** 两个层面都要有。

---

## 九、常见坑与误区 ⚠️

| 坑 / 误区 | 真相 |
|---|---|
| `EXPLAIN` 会执行我的 SQL | 普通 `EXPLAIN` **不执行**，是估算；只有 `EXPLAIN ANALYZE` 真跑 |
| `rows` 是精确要扫的行数 | 是**估算值**，基于统计信息，可能严重失真（用 `ANALYZE` 核实） |
| `type=index` 是"走了索引"的好状态 | 它是**扫整棵索引树**，不是高效定位，要警惕 |
| 看到 `ALL` 就一定有问题 | 小表全表扫可能最优；要结合 `rows` 判断 |
| `Using filesort` = 排序到磁盘文件 | 只表示"没用索引排序、要额外排一次"，可能在内存排 |
| `Using index` 和 `Using index condition` 一样 | 前者**覆盖不回表**，后者**ICP 减少回表**，不同 |
| `key_len` 不用管 | 它是**反推联合索引用到第几列**的关键度量 |
| `LIMIT 1000000,10` 只取 10 行应该很快 | 要**先扫 100 万行再丢弃**，深分页极慢 |
| 慢日志逐行读 | 要按 SQL 模板**聚合**（`mysqldumpslow`/`pt-query-digest`），按总耗时排序 |
| 优化靠"感觉变快了" | 必须 **`EXPLAIN` + 实测**做前后对比，用数据说话 |
| SQL 没改却突然变慢 | 可能是**统计信息过期**，`ANALYZE TABLE` 试试 |

---

## 十、小结

| 知识点 | 记住这一句 |
|---|---|
| EXPLAIN 三形态 | 普通=估算、JSON=看成本、ANALYZE=真跑对比估算与真实 |
| `type` | 好坏：`const>eq_ref>ref>range>index>ALL`；盯 `ALL` 和 `index` |
| `key`/`key_len` | `key` 是实际用的索引；`key_len` 反推用到联合索引第几列 |
| `rows`/`filtered` | 估算扫描行数与过滤比，越小越好，是估算值 |
| `Extra` | 盯 `Using filesort`、`Using temporary`；`Using index`=覆盖是好事 |
| 慢查询日志 | 开 `slow_query_log`、调低 `long_query_time`，用工具聚合找 Top N |
| 优化 SOP | 复现→EXPLAIN→定位根因→改→复验→实测对比→评估副作用 |
| 深分页 | `LIMIT` 大偏移=扫一堆再丢；用延迟关联或键集分页 |
| 统计信息 | 估算与真实差距大→`ANALYZE TABLE` 更新统计 |
| 全局观 | 索引之外还有 SQL 改写、缓存、归档、分库分表 |

---

## 十一、自检问题

不查资料，先口头回答：

1. 普通 `EXPLAIN` 和 `EXPLAIN ANALYZE` 最本质的区别是什么？后者对写语句用要注意什么？
2. `type` 从好到坏排一遍。看到 `type=index` 意味着什么？和 `range` 差在哪？
3. `idx(category_id BIGINT NOT NULL, status TINYINT NOT NULL, created_at DATETIME NOT NULL)`，`key_len=9` 说明用到了哪几列？怎么算的？
4. `Extra` 里 `Using filesort` 和 `Using temporary` 分别代表什么？为什么要警惕？
5. `Using index` 和 `Using index condition` 有什么区别？（接 03 章）
6. `rows` 是精确值还是估算值？它和 `EXPLAIN ANALYZE` 的 `actual rows` 差很多时说明什么？
7. 慢查询日志怎么开？为什么 `log_queries_not_using_indexes` 平时不建议开？
8. 为什么 `SELECT * FROM spu ORDER BY id LIMIT 1000000, 10` 很慢？写出两种优化方式。
9. 给一条慢 SQL，你的优化流程（SOP）是怎样的？为什么每步都要"前后对比"？

答不上 2、3、8 的，回看第三节和 7.4——它们是本章最常被面试追问的点。

---

## 十二、延伸阅读

- [MySQL 8.4 Reference · EXPLAIN Output Format](https://dev.mysql.com/doc/refman/8.4/en/explain-output.html)（各列含义与 `type` 全表）
- [MySQL 8.4 Reference · Obtaining Information with EXPLAIN ANALYZE](https://dev.mysql.com/doc/refman/8.4/en/explain.html)
- [MySQL 8.4 Reference · The Slow Query Log](https://dev.mysql.com/doc/refman/8.4/en/slow-query-log.html)
- [MySQL 8.4 Reference · LIMIT Query Optimization](https://dev.mysql.com/doc/refman/8.4/en/limit-optimization.html)（分页优化）
- [MySQL 8.4 Reference · ORDER BY Optimization](https://dev.mysql.com/doc/refman/8.4/en/order-by-optimization.html)（filesort）

> 版本相关行为（`EXPLAIN ANALYZE` 需 8.0.18+、优化器成本模型、`DATETIME` 字节数等）以官方文档为准，本章基于 MySQL 8.4 / InnoDB。具体某条 SQL 走不走某索引，**最终以你环境上的 `EXPLAIN` 为准**。Content was rephrased for compliance with licensing restrictions.

---

## 下一步

完成[作业 04 · EXPLAIN 与慢查询优化实战](../作业/04-EXPLAIN与慢查询优化实战.md)。核心目标：

1. **观察 / 分析线（experimental）**：在真实 MySQL 上灌一批数据，对 03 章设计的索引跑 `EXPLAIN`，亲手读出 `type`/`key`/`key_len`/`Extra`，验证覆盖、回表、filesort、最左前缀断裂；完成 `key_len` 计算题和一次"慢 SQL → 优化 → 前后对比"。
2. 本章以**观察和诊断能力**为主，不新增 delivered 正式代码（真正把 `product-service` 落到 MySQL 是第 10 章的交付重点）。

> 下一章 **M02-05 事务、隔离级别与 MVCC**：从"查得快"转到"并发下读写正确"，开始啃事务这块硬骨头。
