# 练习 01 · 基础 CRUD 与查询

> **数据**：`seed-basic.sql`（category 6 / spu 7 / sku 9，先灌它）
> **练什么**：SELECT / WHERE / ORDER BY / LIMIT / DISTINCT / 聚合 / GROUP BY / HAVING / JOIN / 子查询 / INSERT / UPDATE / DELETE
> **答案政策**：默认只给题目 + 提示 + 期望结果，自己写 SQL 跑；要答案说 `直接给标准答案 训练营 01-题N`

灌数据：
```bash
docker exec -i mysql84 mysql -uroot -proot123456 mall < homework/experimental/sql-practice-lab/seed/seed-basic.sql
```

先熟悉三张表（自己先跑一遍看数据长什么样）：
```sql
SELECT * FROM category;
SELECT * FROM spu;
SELECT * FROM sku;
```

---

## A. 单表查询：SELECT / WHERE / ORDER BY / LIMIT

> **语法骨架**（SQL 基础薄弱先记这个顺序，书写顺序是固定的）：
> `SELECT 列 FROM 表 WHERE 行过滤 GROUP BY 分组 HAVING 组过滤 ORDER BY 排序 LIMIT 条数`
> 记忆：**选什么 → 从哪 → 留哪些行 → 按什么分组 → 组里留哪些 → 怎么排 → 取几条**。

**题 1**　查出所有**上架**（`status=1`）的 SPU 的 `id, name, brand`。
- 提示：`WHERE status = 1`。
- 期望：5 行（下架的 id=4、id=7 不在内）。

**题 2**　查 `spu` 里品牌是 `Apple` 或 `Xiaomi` 的商品，只要 `name, brand`。
- 提示：`brand IN ('Apple','Xiaomi')`，比写两个 `OR` 清爽。
- 期望：iPhone 15 Pro、小米 14、Redmi Note 13、MacBook Air M3、停售样机 X 共 5 行。

**题 3**　查 `sku` 里价格在 `1000` 到 `5000` 之间的记录，按价格从低到高排。
- 提示：`BETWEEN 1000 AND 5000`（含两端）；`ORDER BY price ASC`（`ASC` 可省）。
- 期望：Redmi(1199) 之上没有，MI14(4299)、MI14(4799)……先想清楚哪些落区间。

**题 4**　查最贵的 3 个 SKU 的 `sku_code, price`。
- 提示：`ORDER BY price DESC LIMIT 3`。
- 期望：MacBook(10999)、iPhone 512(9999)、iPhone 256(7999)。

**题 5**　`spu` 表里一共有几个不同的品牌？
- 提示：`COUNT(DISTINCT brand)`。想清楚 `COUNT(brand)` 和 `COUNT(DISTINCT brand)` 的区别。
- 期望：一个数字（Apple/Xiaomi/Nokia/Anker）。

---

## B. 聚合与分组：COUNT / SUM / AVG / MIN / MAX + GROUP BY + HAVING

**题 6**　统计每个 `spu` 有几个 SKU，输出 `spu_id, sku 数量`。
- 提示：`SELECT spu_id, COUNT(*) FROM sku GROUP BY spu_id`。
- 期望：spu 1→3、spu 2→2，其余各 1。

**题 7**　每个 `spu` 的**最低价、最高价、平均价**（保留 2 位小数）。
- 提示：`MIN/MAX/ROUND(AVG(price),2)`，`GROUP BY spu_id`。

**题 8**　找出**拥有 2 个及以上 SKU** 的 spu_id。
- 提示：组过滤要用 `HAVING`（不能用 `WHERE`，因为 `COUNT(*)` 是聚合结果）。`HAVING COUNT(*) >= 2`。
- 期望：spu 1、spu 2。
- 想清楚：**为什么这里不能写 `WHERE COUNT(*) >= 2`？**（WHERE 在分组前、逐行过滤，聚合值还没算出来。）

**题 9**　每个品牌有几个 SPU，按数量倒序。
- 提示：`GROUP BY brand ORDER BY COUNT(*) DESC`。

**题 10**　`sku` 表所有库存加起来是多少？平均库存多少？
- 提示：`SUM(stock)`、`AVG(stock)`，不分组（对全表聚合）。

---

## C. 多表连接：JOIN

> **概念**：`sku.spu_id` 指向 `spu.id`，`spu.category_id` 指向 `category.id`（逻辑外键）。
> - `INNER JOIN`：两边都匹配才出现。
> - `LEFT JOIN`：左表全保留，右表没匹配到的补 `NULL`。

**题 11**　列出每个 SKU 属于哪个 SPU：输出 `sku_code, spu 名称, price`。
- 提示：`sku k JOIN spu s ON k.spu_id = s.id`。
- 期望：9 行（每个 SKU 一行）。

**题 12**　列出每个 SPU 属于哪个**分类**：输出 `spu 名称, 分类名称`。
- 提示：`spu s JOIN category c ON s.category_id = c.id`。

**题 13**　哪些 SPU **一个 SKU 都没有**？（用 LEFT JOIN 找"右表为 NULL"）
- 提示：`spu s LEFT JOIN sku k ON k.spu_id=s.id WHERE k.id IS NULL`。
- 期望：id=7「停售样机 X」（basic 数据里它没挂 SKU）。
- 想清楚：**为什么用 `INNER JOIN` 找不出来它？**

**题 14**　三表连起来：输出 `分类名, 商品名, sku_code, price`，只看上架 SPU。
- 提示：`category c JOIN spu s ... JOIN sku k ...`，`WHERE s.status=1`。

**题 15**　每个分类下有多少个 SPU（含 0 个的分类也要显示）？
- 提示：`category c LEFT JOIN spu s ON s.category_id=c.id GROUP BY c.id, c.name`；用 `COUNT(s.id)` 而不是 `COUNT(*)`（想清楚为什么）。
- 期望：像"配件""电脑"各 1，"手机数码/手机"这种父分类下直接挂的 SPU 为 0。

---

## D. 子查询

**题 16**　查价格高于**全表 SKU 平均价**的 SKU。
- 提示：`WHERE price > (SELECT AVG(price) FROM sku)`。这叫标量子查询（返回一个值）。

**题 17**　查"至少有一个 SKU 价格超过 8000"的 SPU 名称。
- 提示：两种写法都想一下：`WHERE id IN (SELECT spu_id FROM sku WHERE price > 8000)`，或 `EXISTS`。
- 期望：iPhone 15 Pro（9999）、MacBook Air M3（10999）。

**题 18**　查每个 SPU 及其"最高价 SKU 的价格"。
- 提示：关联子查询或 JOIN + GROUP BY 都行，先用 JOIN + `MAX` 做。

---

## E. 增删改：INSERT / UPDATE / DELETE

> ⚠️ 这些会改数据，练完可重灌 `seed-basic.sql` 复原。

**题 19**　给「小米 14」(spu_id=2) 新增一个 SKU：`sku_code='MI14-1T-BLK'`、价格 `5499`、库存 `50`、规格 `{"容量":"1T","颜色":"黑色"}`。
- 提示：`INSERT INTO sku (spu_id, sku_code, price, stock, spec_json) VALUES (...)`；`spec_json` 用 `JSON_OBJECT('容量','1T','颜色','黑色')`。
- 自测：插入后 `SELECT COUNT(*) FROM sku WHERE spu_id=2` 应为 3。
- 想清楚：如果 `sku_code` 写成已存在的会怎样？（`uk_sku_code` 唯一键冲突报错）

**题 20**　把所有 `Xiaomi` 品牌 SPU 下的 SKU 统一涨价 5%（`price = price * 1.05`）。
- 提示：`UPDATE sku SET price = ROUND(price*1.05,2) WHERE spu_id IN (SELECT id FROM spu WHERE brand='Xiaomi')`。
- ⚠️ **执行前先把 `WHERE` 想清楚**——不带 `WHERE` 的 `UPDATE` 会改全表（这是经典生产事故，练习也养成习惯）。
- 自测：涨价前先 `SELECT` 记下原价，改完再查对比。

**题 21**　把下架且无 SKU 的样机 `id=7` 删除。
- 提示：`DELETE FROM spu WHERE id=7`。想清楚：因为是**逻辑外键**、没物理约束，删 spu 时数据库不会拦你，即使它下面有 SKU 也照删——这是逻辑外键的代价（复习 M02-01 决策 4）。

**题 22（软删除）**　把 `id=5`（快充数据线）做**软删除**：不真删，而是把 `deleted_at` 置为当前时间。
- 提示：`UPDATE spu SET deleted_at = NOW() WHERE id=5`。
- 想清楚：软删除后，业务查询要自己带 `WHERE deleted_at IS NULL` 才能"看不见"它——为什么电商大多用软删除而不是物理 `DELETE`？

---

## 自测汇总

做完对着期望结果核对；没有明确期望的题（如 7、18），用 IDEA/DBeaver 看结果是否合理。
卡壳超过 10 分钟的题，记下来，Review 时一起问。
