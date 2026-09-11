# SQL 实操训练营 · 实验脚手架（sql-practice-lab）

> **性质**：知识点实验 / 巩固训练（`homework/experimental/`），**不属于正式交付**，不阻塞任何模块推进。
> **目标**：把 M02 第 01~04 章的理论（建模、InnoDB/B+树、索引、EXPLAIN）在**真实 MySQL** 上练成肌肉记忆，
> 顺带补齐 SQL 基础实操、面试高频题、易踩坑清单。
> **配套讲解**：[理论学习 · M02 · SQL 实操训练营](../../../理论学习/M02-MySQL与持久层/00-SQL实操训练营/README.md)

这是一个**可反复重置**的练习环境：一个 MySQL 8.4 容器 + 商品域三张表 + 两套种子数据 + 分主题练习。
你在这里怎么改、怎么删都行，练废了一键重灌。

---

## 一、环境（已就绪）

M02 已用 Docker 起好 MySQL 8.4，无需重复安装：

| 项 | 值 |
|----|----|
| 容器名 | `mysql84` |
| 版本 | MySQL 8.4（`utf8mb4` / `utf8mb4_0900_ai_ci`） |
| 端口 | `localhost:3306` |
| 账号 / 密码 | `root` / `root123456` |
| 练习库 | `mall` |
| 数据卷 | `mysql84-data`（重启容器数据不丢） |

> 如果容器不在了（`docker ps` 看不到 `mysql84`），用这条重新起（见本文件末尾"附录：起容器命令"）。

### 三种连接方式（任选）

1. **命令行进容器**（最省事）：
   ```bash
   docker exec -it mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall
   ```
2. **执行单条 / 脚本**：
   ```bash
   docker exec -i mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall -e "SELECT VERSION();"
   docker exec -i mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall < 某脚本.sql
   ```

> **务必带 `--default-character-set=utf8mb4`**。不带的话客户端默认按 `latin1` 连，中文显示乱码、甚至**写入时被存成乱码**（双重编码）。种子脚本里已加 `SET NAMES utf8mb4;` 兜底，但命令行手敲时养成带这个参数的习惯。
3. **IDEA / DBeaver 图形界面**：新建 MySQL 数据源，Host `localhost`、Port `3306`、User `root`、Password `root123456`、Database `mall`。图形界面看结果最直观，**强烈推荐练习时用它**。

> ⚠️ 命令行用 `-e "中文别名"` 可能因客户端字符集显示乱码（不影响数据本身）。练习看结果优先用 IDEA/DBeaver，或加 `--default-character-set=utf8mb4`。

---

## 二、表结构与两套数据

**表结构**直接复用正式交付的商品域三表（单一事实来源，不在 lab 里另造）：
`../../delivered/product-service/docs/db/schema-category.sql` / `schema-spu.sql` / `schema-sku.sql`
（`category` 分类树、`spu` 商品、`sku` 库存单位；索引：`idx_parent_sort`、`idx_category_status_created`、`idx_spu_id`）

**两套种子数据**（在 `seed/`，互斥，按练习需要切换；两者都会先 `TRUNCATE` 三张表）：

| 种子 | 数据量 | 用途 |
|------|--------|------|
| `seed/seed-basic.sql` | category 6 / spu 7 / sku 9 | **基础 CRUD、建模、JOIN、聚合**——数据小、看得懂每一行 |
| `seed/seed-explain.sql` | category 50 / spu 10万 / sku 30万 | **索引、EXPLAIN、深分页、慢查询**——数据大才看得出快慢差异 |

灌数据（脚本已含 `SET NAMES utf8mb4;`，中文不会乱码）：

```bash
cd <仓库根目录>
# 基础练习用小数据
docker exec -i mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall < homework/experimental/sql-practice-lab/seed/seed-basic.sql
# 索引/EXPLAIN 练习用大数据（约 2 秒）
docker exec -i mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall < homework/experimental/sql-practice-lab/seed/seed-explain.sql
```

### 重置 / 重建（练废了怎么办）

- **只重灌数据**：再跑一次对应的 `seed-*.sql`（它们会先 `TRUNCATE`）。
- **表结构也乱了，重建三张表**：按 `category → spu → sku` 顺序重跑 delivered 的 schema：
  ```bash
  cd homework/delivered/product-service/docs/db
  for f in schema-category.sql schema-spu.sql schema-sku.sql; do
    docker exec -i mysql84 mysql -uroot -proot123456 mall < "$f"
  done
  ```
  （schema 里是 `CREATE TABLE`，若表已存在需先 `DROP TABLE sku, spu, category;` 再建。）

---

## 三、练习地图（建议顺序）

练习都在 `exercises/`，每份文件：**题目 + 提示 + 期望结果/自测方式**。按下面顺序刷：

| 顺序 | 文件 | 对应章节 | 用哪套数据 | 练什么 |
|------|------|---------|-----------|--------|
| 1 | `exercises/01-基础CRUD与查询.md` | 基础实操 | basic | SELECT/WHERE/ORDER/LIMIT/聚合/GROUP BY/HAVING/JOIN/子查询/增删改 |
| 2 | `exercises/02-建模与数据类型.md` | M02-01 | basic | 读写 DDL、类型选型、范式/反范式、约束、SPU/SKU |
| 3 | `exercises/03-InnoDB与索引观察.md` | M02-02 / 03 | explain | 聚簇/二级/回表、最左前缀、覆盖、失效（配 EXPLAIN 观察） |
| 4 | `exercises/04-EXPLAIN与慢查询.md` | M02-04 | explain | type/key_len/Extra 读法、深分页、慢日志、优化前后对比 |
| 5 | `exercises/05-面试高频题.md` | 综合 | 任意 | 概念口述 + 现场写 SQL，模拟面试追问 |
| 6 | `exercises/06-易踩坑清单.md` | 综合 | basic+explain | NULL、隐式转换、count、浮点、深分页、LIKE、OR、时区…… |

> 建议每份先自己写、自己跑、对着"期望结果"自测；卡住了看"提示"；还不行再来找我。

---

## 四、答案政策与 Review（重要）

按仓库教学规则，**练习默认只给题目 + 提示 + 期望结果，不直接给标准答案 SQL**——SQL 是练出来的，直接抄答案等于没练。

- 想让我**批改你写的 SQL**：贴上你的 SQL + 运行结果，说 `Review 训练营 0X-题N`，我按"结论 / 错误 / 改进方向 / 架构视角"给反馈，默认不直接给答案。
- 想**直接要参考答案**：明确说 `直接给标准答案 训练营 0X-题N`，我再给（这是你主动触发的例外）。
- 想**换讲法**：`按初学者角度讲解` / `压力测试我`（我连环追问）都可用。

> SQL 基础薄弱是你说过的短板，所以本训练营的讲解会比其它章更细：语法逐段拆、坑逐个点。但"成品答案"仍默认留给你自己写。

---

## 五、中文乱码排查（踩过一次，这里说清）

乱码分两种，**修法完全不同，先判断是哪种**：查一行看 `HEX`——

```bash
docker exec mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall \
  -e "SELECT name, HEX(name) FROM category LIMIT 1;"
```

- 「分类001」的正确 UTF-8 是 `E58886E7B1BB303031`。

### 情况 A：数据是好的，只是显示乱

`HEX` 正确（`E58886...`）但界面显示乱 → **纯显示问题**，数据没坏。
- **命令行**：连接时加 `--default-character-set=utf8mb4`。
- **IDEA / DBeaver 数据网格**：数据源属性里把连接编码设为 `utf8mb4`（IDEA 通常自动识别；不行就在 Advanced 里加 `characterEncoding=UTF-8`）。

### 情况 B：数据真的存坏了（双重编码）

`HEX` 是 `C3A5CB86E280A0...` 这种（比正确的长一截）→ **写入时就存错了**，光改显示救不回来。
- 根因：导入时客户端按 `latin1` 把 UTF-8 字节又编码了一遍。
- 修法：用 `--default-character-set=utf8mb4` **重新灌**（种子脚本已加 `SET NAMES utf8mb4;` 兜底）：
  ```bash
  docker exec -i mysql84 mysql -uroot -proot123456 --default-character-set=utf8mb4 mall \
    < homework/experimental/sql-practice-lab/seed/seed-explain.sql
  ```
  表结构（含中文注释）也想干净，先按第二节"重建三张表"用同样带 `--default-character-set=utf8mb4` 的方式重跑 schema。

### IDEA 打开 `.sql` 文件本身显示乱码

这是 **IDEA 的文件编码设置**（和数据库无关，文件本身是 UTF-8）。
- 右下角状态栏点当前编码 → 选 `UTF-8` → `Reload`（按 UTF-8 重新读）。
- 一劳永逸：`Settings → Editor → File Encodings`，把 Global / Project Encoding 都设成 `UTF-8`。

---

## 六、附录：起 MySQL 容器命令（容器丢了才用）

```bash
docker run -d --name mysql84 \
  -e MYSQL_ROOT_PASSWORD=root123456 \
  -e MYSQL_DATABASE=mall \
  -e TZ=Asia/Shanghai \
  -p 3306:3306 \
  -v mysql84-data:/var/lib/mysql \
  mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci
```

起完等它就绪（约 10~20 秒），再按第二节建表 + 灌数据：

```bash
docker exec mysql84 mysqladmin ping -uroot -proot123456   # 看到 "mysqld is alive" 即就绪
```
