# 练习 04 · EXPLAIN 与慢查询优化（对应 M02-04）

> **数据**：`seed-explain.sql`（spu 10万 / sku 30万）
> **练什么**：逐列读 EXPLAIN、算 key_len、深分页优化、慢日志、"优化前后对比"
> **答案政策**：默认给题目 + 提示 + 期望信号；要答案说 `直接给标准答案 训练营 04-题N`

> 本练习和 M02-04 正式作业高度重合，可以合并做。这里更偏"反复练手感"。

---

## A. EXPLAIN 三种形态

**题 1**　同一条 SQL 分别用三种形态跑，说出各自看什么：
```sql
EXPLAIN                SELECT id,name FROM spu WHERE category_id=3 AND status=1 ORDER BY created_at DESC\G
EXPLAIN FORMAT=JSON    SELECT id,name FROM spu WHERE category_id=3 AND status=1 ORDER BY created_at DESC\G
EXPLAIN ANALYZE        SELECT id,name FROM spu WHERE category_id=3 AND status=1 ORDER BY created_at DESC\G
```
- 提示：普通=估算表格；JSON=看 `query_cost` 等成本细节；ANALYZE=**真跑**，看 `actual time` / 实际 rows。
- ⚠️ `EXPLAIN ANALYZE` 会真正执行，别对 `UPDATE`/`DELETE` 用。

---

## B. type 与 key_len

**题 2**　造出这几种 `type` 各一条 SQL，并验证：`const`、`ref`、`range`、`index`、`ALL`。
- 提示：
  - `const`：`WHERE id = 12345`
  - `ref`：`WHERE category_id=3 AND status=1`（普通索引等值）
  - `range`：`WHERE category_id > 40`
  - `index`：`SELECT id FROM spu WHERE status=1`（覆盖 → 全索引扫）
  - `ALL`：`SELECT * FROM spu WHERE status=1`（回表且缺最左列 → 全表扫）

**题 3**　`key_len` 反推：跑下面三条，记录 `key_len`，手算验证：
```sql
EXPLAIN SELECT id FROM spu WHERE category_id=3\G
EXPLAIN SELECT id FROM spu WHERE category_id=3 AND status=1\G
EXPLAIN SELECT id FROM spu WHERE category_id=3 AND status=1 AND created_at>'2026-02-01'\G
```
- 期望：`8` / `9` / `14`。手算：`BIGINT UNSIGNED NOT NULL`=8、`+TINYINT NOT NULL`=1、`+DATETIME NOT NULL`=5。

---

## C. Extra：filesort / temporary / covering

**题 4**　造出这几种 `Extra` 各一条：`Using index`、`Using filesort`、`Using where`、`Using temporary`。
- 提示：
  - `Using index`：`SELECT id FROM spu WHERE category_id=3 AND status=1`
  - `Using filesort`：`SELECT id FROM spu WHERE category_id=3 ORDER BY created_at DESC`
  - `Using temporary`：`SELECT status, COUNT(*) FROM spu GROUP BY status`（分组常触发临时表）——实际跑看看它显不显示。

---

## D. 深分页优化 ⭐（面试高频）

**题 5**　对比三种分页写法的**真实耗时**（用 `EXPLAIN ANALYZE` 看 `actual time`）：
```sql
-- ① 大偏移（慢）：要扫 50010 行再丢弃前 50000
EXPLAIN ANALYZE SELECT * FROM spu ORDER BY id LIMIT 50000, 10\G
-- ② 延迟关联：子查询只在索引上扫 id，回表只发生在最后 10 行
EXPLAIN ANALYZE SELECT s.* FROM spu s JOIN (SELECT id FROM spu ORDER BY id LIMIT 50000,10) t ON s.id=t.id\G
-- ③ 键集分页：直接用主键定位断点，不扫被跳过的行
EXPLAIN ANALYZE SELECT * FROM spu WHERE id > 50000 ORDER BY id LIMIT 10\G
```
- 期望现象（你的机器数值会不同，但**量级关系应一致**）：
  - ① 会真的扫 5 万多行，耗时最高；
  - ③ 只扫 10 行，**快一到几个数量级**；
  - ② 介于两者之间，但比 ① 明显好。
- 想清楚：
  - 为什么 `LIMIT 50000,10` 慢？（扫 m+n 行丢弃 m 行）
  - 键集分页③ 快在哪、**有什么限制**？（只能上/下一页，不能随机跳页）
  - 什么场景用②、什么场景用③？

---

## E. 慢查询日志

**题 6**　打开慢日志、调低阈值，抓一条慢 SQL：
```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 0.05;                 -- 50ms 就算慢，方便触发
SET GLOBAL log_queries_not_using_indexes = ON;
SHOW VARIABLES LIKE 'slow_query_log_file';         -- 记下路径
```
制造慢查询（如题 5 的 ①，或 `SELECT * FROM spu WHERE status=1`），然后看日志：
```bash
docker exec mysql84 sh -c "tail -n 30 \$(mysql -uroot -proot123456 -N -e 'SELECT @@slow_query_log_file')"
```
- 自测：能在日志里看到刚才那条语句和它的 `Query_time`。
- 用完关掉：`SET GLOBAL long_query_time = 10; SET GLOBAL log_queries_not_using_indexes = OFF;`
- 想清楚：为什么 `log_queries_not_using_indexes` 平时不长期开？（小表全表扫也会被记，日志爆量）

---

## F. 完整的"优化前后对比"（把 SOP 走一遍）

**题 7**　自选一条你觉得慢的查询（比如"某分类下上架商品按创建时间倒序、还要查 name"），按 SOP 完整走一遍并**记录**：
1. 原始 SQL + `EXPLAIN`（type/key/rows/Extra 什么样）；
2. 判断根因（没索引？回表多？filesort？深分页？）；
3. 做一次优化（加/改索引、改写 SQL、或换分页方式）；
4. 优化后 `EXPLAIN` 再看一遍（哪些指标变好了）；
5. `EXPLAIN ANALYZE` 实测前后耗时；
6. 一句话说明这次优化的**副作用**（比如新索引对写入的影响）。

- 这题没有唯一答案，重点是**每一步都有执行计划/耗时证据**，不能凭感觉说"变快了"。

---

## 自测汇总

- A~E 每题贴 `EXPLAIN` 关键列；D、F 贴 `EXPLAIN ANALYZE` 的 `actual time`。
- 深分页（题 5）和 key_len（题 3）是面试最爱追问的，务必练到能脱口而出。
- 做完 F 那道"前后对比"，你就具备了独立诊断线上慢 SQL 的基本功。
