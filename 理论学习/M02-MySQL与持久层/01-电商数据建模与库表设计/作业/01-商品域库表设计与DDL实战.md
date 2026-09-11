# 作业 01 · 商品域库表设计与 DDL 实战

> **对应讲义**：[M02-01 · 电商数据建模与库表设计](../学习/01-电商数据建模与库表设计.md)
> **预计耗时**：3～4 小时
> **难度**：中等
> **Review 模式**：默认"只指出错误，不直接给答案"

> 本作业以**设计 + 可执行 DDL**为主，暂不接入 `product-service` 代码（落地在第 10 章）。
> 正式交付的 DDL 是 M02 后续（第 09 章 Flyway、第 10 章落地）要用的 schema 基础，请当真实项目的库表设计来做。
> 纯练习性质的 SQL 试验放 `homework/experimental/`，不要混进交付的 schema 文件。

---

## 一、背景设定

`product-service` 目前商品数据存在内存 `Map` 里，模型是"无规格单品"（一个 `product` 同时带 `name/sku/price/category`）。M02 要把它搬到 MySQL，并借机把商品域建模做规范：拆出**分类树、SPU、SKU**。

本章先把**库表设计**定下来——这是后面 Flyway 迁移和落地改造的地基，设计错了后面全要返工。

**环境**：需要一个可连接的 MySQL 8.4（Docker 起一个 `mysql:8.4` 即可）。所有 DDL 要能在真实 MySQL 上执行通过。

---

## 二、必做任务

---

### 任务一：商品单品过渡表 `product`

> - 性质：正式交付
> - 实现位置：`homework/delivered/product-service/docs/db/schema-product.sql`
> - 级别：必做
> - 是否阻塞推进：是
> - 前置交付：M01 的 `ProductEntity`（`id/name/sku/price/category/createdAt/updatedAt`）
> - 验证方式：在 MySQL 8.4 上 `SOURCE schema-product.sql` 建表成功，`SHOW CREATE TABLE product` 与设计一致

把 M01 的 `ProductEntity` 原样落成一张规范的 MySQL 表，作为落地改造的过渡模型。要求：

1. 主键、金额、时间、字符集、状态字段的类型都要按讲义第四节的选型；
2. `sku` 加唯一约束；
3. 加上审计字段与状态字段；
4. 每个字段写 `COMMENT`，表用 `InnoDB` + `utf8mb4`。

**验收标准：**

- 在 MySQL 8.4 执行无报错；
- 金额是 `DECIMAL`、主键是 `BIGINT UNSIGNED AUTO_INCREMENT`、时间是 `DATETIME`、字符集 `utf8mb4`；
- `sku` 有唯一索引；不是简单地把所有字段设成 `VARCHAR`。

---

### 任务二：规范商品域三表 `category` / `spu` / `sku`

> - 性质：正式交付
> - 实现位置：`homework/delivered/product-service/docs/db/schema-catalog.sql`
> - 级别：必做
> - 是否阻塞推进：是
> - 前置交付：任务一
> - 验证方式：在 MySQL 8.4 上依次建表成功，能插入一条"1 个 SPU 带 3 个 SKU、挂在某三级分类下"的样例数据

设计规范的多规格商品模型，至少三张表：

1. `category`：分类树，邻接表模型（`id` + `parent_id`），带 `name`、层级或排序字段；
2. `spu`：商品，含 `name`、`category_id`（逻辑外键）、品牌、描述等；
3. `sku`：库存单位，含 `spu_id`（逻辑外键）、`sku_code`（唯一）、`price`、`stock`、销售属性。

要求：

- 关系用**逻辑外键**（字段 `xxx_id`），默认不加物理 `FOREIGN KEY`，并在设计说明里解释为什么；
- 价格、库存落在 `sku`；
- 销售属性（容量/颜色等）自行决定用 JSON 列还是独立规格表，并在说明里讲清取舍；
- 三张表都要有审计字段、软删除字段、`COMMENT`。

**验收标准：**

- 能插入"1 SPU → 3 SKU，挂在三级分类下"的样例数据并查询出来；
- SKU 的 `sku_code` 有唯一索引，价格是 `DECIMAL`；
- 分类树能通过 `parent_id` 表达至少三级层级；
- 索引部分**本章可以先只放主键和唯一键**，普通查询索引留到第 03 章补（作业里注明"索引待 03 章设计"即可，不要求这章就建全）。

---

### 任务三：设计说明文档

> - 性质：正式交付（设计记录）
> - 实现位置：`homework/delivered/product-service/docs/db/商品域建模说明.md`
> - 级别：必做
> - 是否阻塞推进：是
> - 前置交付：任务一、二
> - 验证方式：文档能回答下列取舍问题，与 DDL 一致

用一页左右说明你的设计决策，至少覆盖：

1. 为什么价格和库存放在 `sku` 而不是 `spu`？
2. 分类树为什么用邻接表，而不是其它树模型？
3. 销售属性你选了 JSON 还是独立规格表？各自代价是什么？
4. 为什么用逻辑外键、不加物理外键约束？
5. `product` 过渡表和规范的 `spu/sku` 是什么关系？第 10 章打算怎么平滑迁移？

**验收标准：**

- 每个决策有"为什么"和"代价"，不是只写"我用了 X"；
- 说明与实际 DDL 一致，不自相矛盾。

---

## 三、进阶任务（按需选做）

### 任务四：递归查询分类子树

> - 性质：知识点实验
> - 实现位置：`homework/experimental/catalog-sql-lab/`（自建，放试验 SQL 即可）
> - 级别：进阶
> - 是否阻塞推进：否
> - 验证方式：能查出某分类的所有后代分类

用 MySQL 8 的递归 CTE（`WITH RECURSIVE`）写一条 SQL，查出"手机数码"及其所有子孙分类。体会邻接表"查子树要递归"的代价。

> SQL 基础薄弱不用怕：递归 CTE 的**可背模板 + 逐行详解 + 无限递归/性能坑**已整理在本章 [死记硬背 · 递归 CTE](../学习/死记硬背.md)，照模板改表名和起点条件就能跑。附录 E 也有指引。

### 任务五：字段类型踩坑复现

> - 性质：知识点实验
> - 实现位置：`homework/experimental/catalog-sql-lab/`
> - 级别：进阶
> - 是否阻塞推进：否

建两张小表分别用 `FLOAT` 和 `DECIMAL` 存金额，各插入 `0.1` 和 `0.2` 累加，对比 `SELECT SUM(...)` 的结果差异，用真实输出验证"为什么金额不能用浮点"。

---

## 四、挑战任务（面试 / 答辩级）

### 任务六：订单为什么要冗余商品快照

> - 级别：挑战（只需提交分析，不要求落地）

1. 订单表为什么要冗余下单时的商品名称、单价、SKU 规格，而不是只存 `sku_id` 去 `JOIN`？
2. 这种冗余违反第三范式吗？如果违反，为什么还要这么做？
3. 如果商品改了价，已生成的订单和购物车里的商品，价格应该怎么处理？

---

## 附录 · SQL 写法教学（面向 SQL 基础薄弱）

> 你说目前只能写简单 SQL，这一节把本作业要用到的 SQL 语法拆开教一遍。**这里只教"语法怎么写、思路怎么想"，不直接给正式交付（任务一/二）的成品 DDL**——那属于要你自己完成的交付。实验区的任务四/五给了较完整的示例，因为它们本就是"照着跑、观察现象"的知识点实验。
>
> 递归 CTE 的逐行详解和可背模板见本章 [死记硬背 · 递归 CTE](../学习/死记硬背.md)。

### A. 建表：`CREATE TABLE` 的结构（任务一 / 二）

`CREATE TABLE` 就是"给一张表定义有哪些列、每列什么类型、有什么约束"。用一张**和作业无关的示范表**把结构讲清，你照着这个骨架去写 `product` / `category` / `spu` / `sku` 即可：

```sql
CREATE TABLE `demo`(                                  -- 表名，反引号可选，防止和关键字冲突
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name`       VARCHAR(100)    NOT NULL                COMMENT '名称',   -- NOT NULL = 不允许为空
  `price`      DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '金额',   -- DEFAULT = 不填时的默认值
  `status`     TINYINT         NOT NULL DEFAULT 1      COMMENT '1-启用 0-停用',
  `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),                                 -- 主键：唯一标识一行
  UNIQUE KEY `uk_name` (`name`)                       -- 唯一键：这一列不允许重复
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='示范表';   -- 引擎 + 字符集 + 表注释
```

拆解要记的几件事：

- 每一列格式固定：**`列名 类型 [约束] [DEFAULT 默认值] [COMMENT '注释']`**，列之间用逗号分隔，最后一列后面不要逗号。
- `PRIMARY KEY` / `UNIQUE KEY` 这些**约束单独占一行**，写在所有列的后面。
- 结尾的 `ENGINE / CHARSET / COMMENT` 是**整张表的属性**。
- 类型怎么选（金额 `DECIMAL`、主键 `BIGINT UNSIGNED`…）照讲义第四节和死记硬背的速查表，别一律 `VARCHAR`。

> 任务一的 `product` 表，讲义第 6.1 节已经给了完整 DDL 可对照；任务二的 `category`/`spu`/`sku` 要你**自己套上面这个骨架写**，列名和关系参考讲义 6.2 节的字段清单。

### B. 执行 SQL 文件：`SOURCE`（验证任务一 / 二）

把 DDL 存成 `.sql` 文件后，在 MySQL 命令行里这样跑：

```sql
SOURCE /绝对路径/schema-catalog.sql;      -- 执行整个文件里的建表语句
SHOW CREATE TABLE sku;                     -- 反查某张表的真实结构，贴进交付说明当证据
```

### C. 插入样例数据：`INSERT`（任务二 "1 SPU→3 SKU"）

`INSERT` 就是往表里塞数据。语法是"指定往哪些列、塞什么值"：

```sql
-- 单行插入：列名和值一一对应
INSERT INTO category (name, parent_id) VALUES ('手机数码', 0);

-- 多行插入：VALUES 后面多组括号，逗号隔开（塞 3 个 SKU 就用这种）
INSERT INTO sku (spu_id, sku_code, price, stock) VALUES
  (1, 'SKU-256-BLK', 7999.00, 100),
  (1, 'SKU-256-WHT', 7999.00,  80),
  (1, 'SKU-512-BLK', 9999.00,  30);
```

要点：**没写默认值的列（如自增 `id`、带 `DEFAULT` 的 `created_at`）不用出现在 `INSERT` 里**，数据库会自动填。`spu_id=1` 这种要先插了 SPU 拿到它的 `id`，再拿这个 id 去插 SKU（逻辑外键靠你手动对上）。

### D. 查出来验证：`SELECT` 与 `JOIN`（任务二验收）

```sql
-- 最基础：查一张表
SELECT id, name, price, stock FROM sku WHERE spu_id = 1;

-- JOIN：把 spu 和 sku 按关系拼起来看"一个 SPU 带哪些 SKU"
SELECT s.name AS spu名称, k.sku_code, k.price, k.stock
FROM spu s
JOIN sku k ON k.spu_id = s.id      -- ON 写的就是两表的关联关系（逻辑外键对上）
WHERE s.id = 1;
```

`JOIN ... ON` 的 `ON` 后面写的永远是"两张表靠哪个字段对上"，这里就是 `sku.spu_id = spu.id`。

### E. 递归查子树（任务四，实验区）

这是本作业唯一"简单 SQL 覆盖不到"的写法，模板和逐行详解已放进 [死记硬背 · 递归 CTE](../学习/死记硬背.md)。核心结构一句话：`WITH RECURSIVE 名字 AS ( 起点 UNION ALL 靠 JOIN 自己往下滚 )`。照模板改表名和起点条件即可跑。

### F. 浮点踩坑对比（任务五，实验区）

这个实验就是要你用真实输出证明"金额不能用浮点"，给完整可跑示例：

```sql
-- 建两张最小表，一张用 FLOAT，一张用 DECIMAL
CREATE TABLE money_float   (v FLOAT);
CREATE TABLE money_decimal (v DECIMAL(10,2));

-- 各插入 0.1 和 0.2
INSERT INTO money_float   VALUES (0.1), (0.2);
INSERT INTO money_decimal VALUES (0.1), (0.2);

-- 对比求和结果：FLOAT 会出现精度误差，DECIMAL 精确
SELECT SUM(v) FROM money_float;     -- 观察是不是干净的 0.3
SELECT SUM(v) FROM money_decimal;   -- 应为精确 0.30
```

跑完把两个 `SUM` 的真实输出贴进交付说明，就完成了"用输出验证浮点不能存金额"。

---

## 五、提交检查清单

### 正式交付（product-service/docs/db）

- [ ] `schema-product.sql`：单品过渡表，能在 MySQL 8.4 建表成功
- [ ] `schema-catalog.sql`：`category`/`spu`/`sku` 三表，能插入 1 SPU→3 SKU 样例
- [ ] 金额 `DECIMAL`、主键 `BIGINT UNSIGNED`、时间 `DATETIME`、字符集 `utf8mb4`
- [ ] `sku_code` / `sku` 有唯一索引，逻辑外键无物理约束
- [ ] 审计字段、软删除字段、`COMMENT` 齐全
- [ ] `商品域建模说明.md`：每个设计决策有"为什么 + 代价"
- [ ] 试验性 SQL 放在 experimental，未混进交付 schema

### 验证证据

- [ ] 贴出 `SHOW CREATE TABLE`（product / spu / sku）的结果
- [ ] 贴出"1 SPU→3 SKU"样例数据的查询结果

---

## 六、交付说明（完成后填写）

```markdown
完成时间：2026-09-09（交付门禁核验 + 语法修正）
完成任务：必做 1 / 2 / 3；进阶：未做（不阻塞）；挑战：未做

### 单品过渡表（任务一）
- 文件：schema-product.sql
- 修正项：表名/主键改反引号（原为单引号，MySQL 语法错误）；补 PRIMARY KEY 后缺失的逗号；
  补 status 状态字段（任务必需）；VARCHAR 长度按讲义按需（name 100 / sku 64 / category 50）。
- 建表验证（SHOW CREATE TABLE product）：本章经用户同意跳过真实 MySQL 执行，未粘贴。

### 规范三表（任务二）
- 文件：schema-category.sql / schema-spu.sql / schema-sku.sql（按表拆成三个文件，未用作业里的 schema-catalog.sql 单文件，拆分可接受）
- 修正项：全部表名/主键改反引号；sku 的 spu_id 注释纠正（原误写“主键”）；
  category.level 改 TINYINT UNSIGNED、parent_id 加 DEFAULT 0 表顶级。
- 销售属性选型（JSON / 独立表）及理由：改用 `sku.spec_json` JSON 列（原 `sale_property_id` 单个外键无法表达一个 SKU 多个销售属性，且引用表未定义）。理由与独立规格表备选方案的取舍见设计说明决策 3。
- 1 SPU→3 SKU 样例查询结果：样例 INSERT/SELECT 见 `商品域建模说明.md` 第五节（示意，未实际执行）。

### 设计说明（任务三）
- 文件：商品域建模说明.md
- 关键取舍：价格库存落 SKU、分类邻接表、销售属性用 JSON、逻辑外键不加物理约束、product 过渡表到 spu/sku 的迁移思路，均给出“为什么 + 代价”。

遇到的问题：本机未装 mysql 客户端、docker daemon 未运行；本章重点在表设计而非 SQL 执行，经用户同意跳过真实 MySQL 执行验证，仅修复语法错误与设计缺陷。
尚未完成或需要 review 的部分：进阶任务四/五（递归 CTE、浮点踩坑，实验区，不阻塞）与挑战任务六（订单快照分析）未做。
```

---

## 七、提交后如何请求 Review

完成后直接说：

> Review 作业 01（M02），只指出错误，不直接给答案。

我会按固定格式检查：结论、错误清单、改进建议、架构视角（大数据量/高并发下的建模影响）、可选优化、下一步。
