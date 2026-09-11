# 练习 03 · InnoDB 与索引观察（对应 M02-02 / M02-03）

> **数据**：`seed-explain.sql`（spu 10万 / sku 30万，先灌它——**数据量大才看得出索引效果**）
> **练什么**：聚簇索引 vs 二级索引、回表、最左前缀、覆盖、索引失效，全部用 `EXPLAIN` 亲眼验证
> **答案政策**：默认给题目 + 提示 + 期望信号，自己跑；要答案说 `直接给标准答案 训练营 03-题N`

灌数据 + 更新统计：
```bash
docker exec -i mysql84 mysql -uroot -proot123456 mall < homework/experimental/sql-practice-lab/seed/seed-explain.sql
```

> **怎么看 EXPLAIN**：命令行加 `\G` 竖排更好读，例如
> `docker exec mysql84 mysql -uroot -proot123456 mall -e "EXPLAIN SELECT ...\G"`
> 重点看四列：`type`（访问方式）、`key`（用了哪个索引）、`key_len`（用到联合索引前几列）、`Extra`。

---

## A. 二级索引与回表

**题 1**　对比这两条的 `Extra`，解释差异：
```sql
EXPLAIN SELECT id       FROM spu WHERE category_id = 3 AND status = 1\G
EXPLAIN SELECT id, name FROM spu WHERE category_id = 3 AND status = 1\G
```
- 期望：第一条 `Extra` 有 **`Using index`**（覆盖，不回表）；第二条 `Extra` 没有（要回表取 `name`）。
- 想清楚：为什么只查 `id` 不回表？（`id` 是主键，就在二级索引叶子里）为什么要 `name` 就得回表？

**题 2**　主键查询有多快？
```sql
EXPLAIN SELECT * FROM spu WHERE id = 12345\G
```
- 期望：`type = const`（主键等值，最多一行，直接命中聚簇索引，不回表）。

---

## B. 最左前缀（联合索引 idx_category_status_created）

**题 3**　逐条跑，记录 `type` / `key` / `key_len`，判断"用到了索引的哪几列"：
```sql
EXPLAIN SELECT id FROM spu WHERE category_id = 3\G                                  -- 期望 key_len=8
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1\G                    -- 期望 key_len=9
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1 AND created_at > '2026-02-01'\G  -- 期望 key_len=14
EXPLAIN SELECT * FROM spu WHERE status = 1\G                                         -- 期望 key=NULL, type=ALL
```
- `key_len` 手算：`category_id BIGINT UNSIGNED NOT NULL`=8、`status TINYINT NOT NULL`=1、`created_at DATETIME NOT NULL`=5 → 8 / 9 / 14。
- 第 4 条为什么退化成全表扫？（缺最左列 `category_id`，`status` 全局乱序，用不上联合索引）

**题 4（易错坑，务必亲手验证）**　把第 3 题第 4 条的 `SELECT *` 换成 `SELECT id`：
```sql
EXPLAIN SELECT id FROM spu WHERE status = 1\G
```
- 期望：`type` 不是 `ALL` 而是 **`index`**，`Extra` 有 `Using index`！
- 想清楚：为什么只改了 `SELECT` 的列，`type` 就从 `ALL` 变 `index`？（`SELECT id` 被联合索引覆盖，扫二级索引比扫整表窄，优化器选全索引扫描）
- **结论**：`type` 会被 `SELECT` 的列影响；要观察"缺最左列→全表扫"，得用 `SELECT *`。

**题 5**　`WHERE` 里把列的书写顺序调换，会影响走不走索引吗？
```sql
EXPLAIN SELECT id FROM spu WHERE status = 1 AND category_id = 3\G
```
- 期望：和 `category_id=3 AND status=1` 一样走索引（`key_len=9`）。
- 结论：决定能否用索引的是**索引里列的顺序**，不是 WHERE 书写顺序（优化器会自动调）。

---

## C. 排序与 filesort

**题 6**　对比 filesort 的产生与消除：
```sql
EXPLAIN SELECT id FROM spu WHERE category_id = 3 ORDER BY created_at DESC\G            -- 期望 Using filesort
EXPLAIN SELECT id FROM spu WHERE category_id = 3 AND status = 1 ORDER BY created_at DESC\G  -- 期望无 filesort
```
- 期望：第一条 `Extra` 含 `Using filesort`；第二条不含（可能显示 `Backward index scan`，说明顺着索引反向读）。
- 想清楚：为什么补上 `status=1` 后排序就能走索引了？（联合索引中间的 `status` 不能跳，固定它之后 `created_at` 才在该段有序）

---

## D. 索引失效（每条都用 EXPLAIN 验证 key 变没变 NULL）

**题 7**　函数包裹列：
```sql
EXPLAIN SELECT * FROM spu WHERE DATE(created_at) = '2026-02-01'\G   -- 失效，key=NULL
```
改写成走索引的形式（范围），再 EXPLAIN 验证 `key` 命中：
- 提示：`created_at >= '2026-02-01 00:00:00' AND created_at < '2026-02-02 00:00:00'`。

**题 8**　隐式类型转换：
```sql
EXPLAIN SELECT * FROM sku WHERE sku_code = 202609\G     -- sku_code 是 VARCHAR，用数字比 → 失效
EXPLAIN SELECT * FROM sku WHERE sku_code = '202609'\G   -- 类型对齐 → 走 uk_sku_code
```
- 期望：前者 `key=NULL type=ALL`，后者走索引。

**题 9**　`LIKE` 通配符位置：
```sql
EXPLAIN SELECT * FROM sku WHERE sku_code LIKE 'SKU-000123%'\G   -- 前缀确定，能走索引
EXPLAIN SELECT * FROM sku WHERE sku_code LIKE '%000123'\G       -- % 开头，用不上前缀
```
- 想清楚：为什么 `'x%'` 能走、`'%x'` 不能？（索引按字符从左到右排序）

---

## E. 选择性与"该不该建索引"

**题 10**　`status` 只有 0/1。查它的选择性：
```sql
SELECT COUNT(DISTINCT status) / COUNT(*) AS 选择性 FROM spu;
```
- 期望：一个非常小的数（≈2/10万）。
- 想清楚：为什么不给 `status` **单独**建索引？就算建了优化器为什么多半不走？（选择性太差，走索引回表还不如全表扫）

**题 11**　`SHOW INDEX FROM spu;` 看每个索引的 `Cardinality`（基数≈不同值个数）。
- 想清楚：`idx_category_status_created` 里 `category_id`、`status`、`created_at` 谁的基数高？为什么高基数列适合放联合索引左边？

---

## 自测汇总

- 每题都要贴 `EXPLAIN` 的 `type`/`key`/`key_len`/`Extra` 四列，光记结论不算数。
- 题 3、4、6、8 是最容易考、最容易错的，务必亲手跑、看现象、能解释。
- 你环境的 `rows`/`filtered` 数值可能和这里略有出入（数据分布不同），但 `type`/`key`/`Extra` 的结论应一致。
