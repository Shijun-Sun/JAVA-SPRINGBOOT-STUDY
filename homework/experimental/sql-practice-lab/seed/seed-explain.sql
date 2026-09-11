-- ============================================================
-- seed-explain.sql · EXPLAIN / 索引 / 深分页 练习用大数据种子
-- 面向 M02-04 作业与 SQL 实操训练营
-- 目标数据量：category 50、spu 100000、sku 约 300000
-- 用法：docker exec -i mysql84 mysql -uroot -proot123456 mall < 本文件
-- 说明：这是"实验区"数据，可反复重灌；会先 TRUNCATE 三张表
-- ============================================================

-- 关键：强制本会话按 utf8mb4 解析，避免客户端默认 latin1 把中文存成乱码（双重编码）
SET NAMES utf8mb4;

-- 递归 CTE 生成大量行，需临时放开递归深度上限（默认 1000）
SET SESSION cte_max_recursion_depth = 1000000;

-- 先清空（TRUNCATE 比 DELETE 快，且重置自增；注意会清掉表内已有数据）
TRUNCATE TABLE sku;
TRUNCATE TABLE spu;
TRUNCATE TABLE category;

-- 1) 分类：50 个一级分类（parent_id=0），sort 递增
INSERT INTO category (parent_id, name, level, sort)
WITH RECURSIVE t(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM t WHERE n < 50
)
SELECT 0, CONCAT('分类', LPAD(n, 3, '0')), 1, n * 10
FROM t;

-- 2) SPU：10 万行
--    category_id 落在 1..50、status 约 20% 下架、created_at 按分钟递增（便于范围/排序练习）
INSERT INTO spu (category_id, name, brand, status, created_at)
WITH RECURSIVE t(n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM t WHERE n < 100000
)
SELECT
    (n % 50) + 1                              AS category_id,
    CONCAT('商品-', LPAD(n, 6, '0'))          AS name,
    CONCAT('品牌', LPAD((n % 20) + 1, 2, '0')) AS brand,
    IF(n % 5 = 0, 0, 1)                        AS status,
    TIMESTAMP('2026-01-01 00:00:00') + INTERVAL n MINUTE AS created_at
FROM t;

-- 3) SKU：每个 SPU 挂 3 个，约 30 万行
--    sku_code 用 spu_id + 序号保证唯一
INSERT INTO sku (spu_id, sku_code, price, stock, status)
SELECT
    s.id,
    CONCAT('SKU-', LPAD(s.id, 6, '0'), '-', k.n),
    ROUND(100 + RAND() * 9900, 2),
    FLOOR(RAND() * 1000),
    1
FROM spu s
JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3) k;

-- 4) 灌完更新统计信息，保证 EXPLAIN 的 rows 估算靠谱
ANALYZE TABLE category, spu, sku;

-- 5) 核对量级
SELECT 'category' AS tbl, COUNT(*) AS cnt FROM category
UNION ALL SELECT 'spu', COUNT(*) FROM spu
UNION ALL SELECT 'sku', COUNT(*) FROM sku;
