# 作业 04 · EXPLAIN 与慢查询优化实战

> **对应讲义**：[M02-04 · EXPLAIN 与慢查询优化](../学习/04-EXPLAIN与慢查询优化.md)
> **预计耗时**：3～4 小时
> **难度**：中等（以真实 MySQL 上的观察 / 诊断为主）
> **Review 模式**：默认"只指出错误，不直接给答案"

> **本章作业性质说明（重要）**
> 本章是**诊断与调优能力训练**，几乎全部是**知识点实验 / 观察**，落 `homework/experimental/`：
> - 你已经用 Docker 起了 `mysql:8.4`（容器 `mysql84`、库 `mall`，03 章的三张表已建好、索引已补齐）。
> - 本章**不新增 delivered 正式代码**。真正把 `product-service` 落到 MySQL 是第 10 章的交付重点；本章先把"读执行计划、找慢 SQL、优化并验证"练熟。
> - 环境和种子数据统一用 **[SQL 实操训练营](../../00-SQL实操训练营/README.md)** 的 `homework/experimental/sql-practice-lab/`，别再各自造数据。

---

## 一、前置：准备一个"有数据、看得出差异"的环境

空表跑 `EXPLAIN` 看不出所以然。先灌数据：

```bash
# 训练营已备好种子脚本，一条命令灌 10 万级 spu + 若干 sku（详见 lab 的 README）
docker exec -i mysql84 mysql -uroot -proot123456 mall \
  < homework/experimental/sql-practice-lab/seed/seed-explain.sql
```

灌完确认量级：

```sql
SELECT COUNT(*) FROM spu;   -- 期望：10 万级
SELECT COUNT(*) FROM sku;   -- 期望：数十万
ANALYZE TABLE spu, sku, category;   -- 灌数后先更新统计信息，保证 EXPLAIN 估算靠谱
```

> 没有数据、或数据太少（几十行），优化器会直接全表扫，`EXPLAIN` 现象和讲义对不上——**这一步必须做到位**。

---

## 二、观察线（必做，落 experimental）

> 落位：`homework/experimental/explain-lab/`（自建），每题贴**原始 `EXPLAIN` 输出** + 你的解读。
> - 性质：知识点实验（机制观察）
> - 级别：必做
> - 是否阻塞推进：否（但这是进第 05 章的**理解门槛**）
> - 验证方式：输出真实、解读正确

### 任务一：读懂 `type` —— 从 `ref` 到 `ALL`

对下面每条 `EXPLAIN`，记录 `type` / `key` / `rows` / `Extra`，并用一句话说明"为什么是这个 `type`"：

```sql
EXPLAIN SELECT * FROM spu WHERE id = 100;                       -- 期望 type=const
EXPLAIN SELECT id,name FROM spu WHERE category_id = 3 AND status = 1;  -- 期望 type=ref
EXPLAIN SELECT * FROM spu WHERE category_id > 3;                -- 期望 type=range 或 ALL（看数据分布）
EXPLAIN SELECT * FROM spu WHERE status = 1;                     -- 期望 type=ALL（缺最左列）
```

**验收**：四条 `type` 判断正确；能解释第 4 条为什么退化成 `ALL`（联想 03 章最左前缀）。

### 任务二：用 `key_len` 反推"用到联合索引第几列"

对 `idx_category_status_created (category_id, status, created_at)`，分别跑并记录 `key_len`：

```sql
EXPLAIN SELECT id FROM spu WHERE category_id = 3;
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1;
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1 AND created_at > '2026-01-01';
```

**验收**：
- 三条的 `key_len` 分别约为 `8` / `9` / `14`（`BIGINT UNSIGNED NOT NULL`=8、`TINYINT NOT NULL`=1、`DATETIME NOT NULL`=5）；
- 写出你的**手算过程**，并解释 `key_len` 怎么反映"最左前缀走到第几列"。

### 任务三：覆盖 vs 回表（`Using index` 的有无）

```sql
EXPLAIN SELECT id       FROM spu WHERE category_id = 3 AND status = 1;   -- 期望 Extra 有 Using index
EXPLAIN SELECT id, name FROM spu WHERE category_id = 3 AND status = 1;   -- 期望 Extra 无 Using index（要回表取 name）
```

**验收**：贴两条输出，指出 `Extra` 差异，一句话解释"为什么第二条要回表"。

### 任务四：`Using filesort` 的产生与消除

```sql
-- A：跳过 status 直接按 created_at 排 → 期望 Using filesort
EXPLAIN SELECT id FROM spu WHERE category_id = 3 ORDER BY created_at DESC;
-- B：补上 status 等值 → 期望 filesort 消失（走索引排序）
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1 ORDER BY created_at DESC;
```

**验收**：贴 A/B 输出，说明 filesort 为什么在 A 出现、在 B 消失（联想 03 章"排序进前缀 + 中间不能断"）。

### 任务五：深分页的诊断与优化 ⭐

```sql
-- 慢：大偏移
EXPLAIN ANALYZE SELECT * FROM spu ORDER BY id LIMIT 50000, 10;
-- 优化一：延迟关联
EXPLAIN ANALYZE
SELECT s.* FROM spu s
JOIN (SELECT id FROM spu ORDER BY id LIMIT 50000, 10) t ON s.id = t.id;
-- 优化二：键集分页（用上一页最后一个 id，假设是 50000）
EXPLAIN ANALYZE SELECT * FROM spu WHERE id > 50000 ORDER BY id LIMIT 10;
```

**验收**：
- 用 `EXPLAIN ANALYZE` 的 **`actual time`** 对比三者真实耗时；
- 解释"为什么大偏移慢"（扫 m+n 行丢弃 m 行）、两种优化各自的原理和**适用/限制**（键集分页不能随机跳页）。

---

## 三、诊断线（必做，落 experimental）

### 任务六：开慢查询日志，抓一条慢 SQL

> - 性质：知识点实验（机制观察）
> - 级别：必做 / 是否阻塞：否

```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 0.1;              -- 阈值调低，方便触发
SET GLOBAL log_queries_not_using_indexes = ON;
SHOW VARIABLES LIKE 'slow_query_log_file';     -- 记下路径
```

制造一条慢查询（如任务四的 A、或对无索引列的查询），然后：

```bash
# 在容器里看慢日志尾部
docker exec mysql84 sh -c "tail -n 40 \$(mysql -uroot -proot123456 -N -e \"SELECT @@slow_query_log_file\")"
```

**验收**：贴出慢日志里记录的那条语句片段（含 `Query_time`），说明你是怎么触发并定位到它的。用完把阈值和 `log_queries_not_using_indexes` 关掉（`SET GLOBAL ... = OFF` / 恢复默认），说明为什么平时不长期开。

### 任务七：统计信息过期的现象（进阶，选做）

> - 级别：进阶 / 阻塞：否

1. 大量 `DELETE` 或 `INSERT` 后，`SHOW INDEX FROM spu` 看 `Cardinality`；
2. 用 `EXPLAIN ANALYZE` 对比某查询的**估算 `rows`** 与 **`actual rows`**；
3. 执行 `ANALYZE TABLE spu` 后再看一次，记录变化。

**验收**：说明"估算与真实差距大"意味着什么、`ANALYZE TABLE` 做了什么。

---

## 四、分析题（必做理解，纯文档，落 experimental）

> 落位：`homework/experimental/explain-lab/EXPLAIN分析题.md`

### 任务八：`key_len` 计算题

给定索引与列定义，**手算** `key_len`（写出每列字节的来源）：

1. `idx(a INT NOT NULL, b VARCHAR(20) NOT NULL, c TINYINT NULL)`，`utf8mb4`：
   - `WHERE a=? ` → key_len=？
   - `WHERE a=? AND b=?` → key_len=？
   - `WHERE a=? AND b=? AND c=?` → key_len=？
2. 若把 `a` 改成 `a INT NULL`，上面第 1 问的 key_len 变成多少？为什么 +1？

**验收**：每问给出数字 + 字节来源拆解（基础字节 / NULL 标记 +1 / 变长 +2）。

### 任务九：给一条"看起来没问题却慢"的 SQL 做体检

对下面每条，指出 `EXPLAIN` 里**最可能暴露问题的信号**，以及**根因 + 改法**（假设相关列本应有索引）：

```sql
-- 1
SELECT * FROM sku WHERE sku_code = 202609;
-- 2
SELECT * FROM spu WHERE YEAR(created_at) = 2026;
-- 3
SELECT * FROM spu WHERE category_id = 3 OR name = 'iPhone';
-- 4
SELECT * FROM spu ORDER BY created_at DESC LIMIT 100000, 20;
```

**验收**：每条写出"`EXPLAIN` 会看到什么（type/key/Extra）→ 根因 → 改写"，且改法能用 `EXPLAIN` 验证生效。

---

## 五、挑战任务（面试 / 答辩级，只需分析）

### 任务十：`EXPLAIN ANALYZE` 树怎么读

用一条带 JOIN 或子查询的语句跑 `EXPLAIN ANALYZE`，解释输出树里 `actual time=A..B`、`rows`、`loops` 各是什么，`loops>1` 通常出现在什么位置、说明什么。

### 任务十一：优化的边界

1. 一条查询 `EXPLAIN` 已经 `type=ref`、`Using index`、`rows` 也不大，但业务仍嫌慢，还能从哪些**非索引**角度优化？（联想讲义第八节）
2. 什么情况下"加索引"反而是错的选择？（写多读少、低选择性、索引已过多）

---

## 六、提交检查清单

### 知识点实验（experimental/explain-lab/）

- [ ] 任务一：四条 `type` 判断 + 输出
- [ ] 任务二：三条 `key_len` + 手算过程
- [ ] 任务三：覆盖 vs 回表的 `Using index` 差异
- [ ] 任务四：`Using filesort` 的产生与消除
- [ ] 任务五：深分页三种写法的 `EXPLAIN ANALYZE` 真实耗时对比
- [ ] 任务六：慢日志触发 + 定位 + 用完关闭
- [ ] 任务八：`key_len` 计算题
- [ ] 任务九：四条 SQL 体检
- [ ]（进阶）任务七 统计信息、任务十/十一 挑战

> 观察/诊断类每题都要贴**原始 `EXPLAIN` 输出**，不能只写结论。

---

## 七、交付说明（完成后填写）

```markdown
完成时间：
环境：mysql84 容器 / mall 库 / spu 数据量约 __ 万
完成任务：观察 1-5；诊断 6(7 选做)；分析 8-9；挑战 10-11：做 / 未做

### 观察线
- type 四条：const / ref / range|ALL / ALL —— 输出见……
- key_len：8 / 9 / 14，手算：……
- 覆盖 vs 回表：SELECT id 出 Using index / SELECT id,name 无 —— 见……
- filesort：A 出现 / B 消失，原因……
- 深分页：LIMIT 50000,10 actual __ms / 延迟关联 __ms / 键集 __ms

### 诊断线
- 慢日志：触发的语句 Query_time=__，定位过程……
- （进阶）统计信息：ANALYZE 前后 rows 估算变化……

### 分析题
- key_len 计算 1/2：……
- SQL 体检 1-4：……

遇到的问题：
尚未完成 / 需 review 的部分：
```

---

## 八、提交后如何请求 Review

完成后直接说：

> Review 作业 04（M02），只指出错误，不直接给答案。

我会按固定格式检查：结论、错误清单（定位到你读错的执行计划列或算错的 key_len）、改进建议、架构视角（大数据量 / 高并发 / 写读比下的索引与优化取舍）、可选优化、下一步。

> 本章 Review 会特别盯：**`type` 读得准不准、`key_len` 会不会算、`filesort`/覆盖分不分得清、深分页优化讲不讲得透**——这些是"能不能独立诊断线上慢 SQL"的分水岭。
