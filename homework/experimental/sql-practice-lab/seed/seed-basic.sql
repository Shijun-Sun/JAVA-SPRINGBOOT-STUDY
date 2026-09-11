-- ============================================================
-- seed-basic.sql · 基础 CRUD / 建模 / JOIN / 聚合 练习用小数据集
-- 特点：数据量小、可读、看得懂每一行，适合练"写对 SQL"
-- 用法：docker exec -i mysql84 mysql -uroot -proot123456 mall < 本文件
-- 注意：会先 TRUNCATE 三张表，和 seed-explain.sql（大数据）互斥，按需切换
-- ============================================================

-- 关键：强制本会话按 utf8mb4 解析，避免客户端默认 latin1 把中文存成乱码（双重编码）
SET NAMES utf8mb4;

TRUNCATE TABLE sku;
TRUNCATE TABLE spu;
TRUNCATE TABLE category;

-- 分类树：手机数码(1) → 手机(2)/电脑(6)；手机 → 智能手机(3)/老人机(4)
INSERT INTO category (id, parent_id, name, level, sort) VALUES
  (1, 0, '手机数码', 1, 10),
  (2, 1, '手机',     2, 10),
  (3, 2, '智能手机', 3, 10),
  (4, 2, '老人机',   3, 20),
  (5, 1, '配件',     2, 20),
  (6, 1, '电脑',     2, 30);

-- SPU：挂在不同分类下，含上架/下架、不同品牌、不同创建时间
INSERT INTO spu (id, category_id, name, brand, status, created_at) VALUES
  (1, 3, 'iPhone 15 Pro',   'Apple',   1, '2026-01-05 10:00:00'),
  (2, 3, '小米 14',          'Xiaomi',  1, '2026-02-10 11:00:00'),
  (3, 3, 'Redmi Note 13',   'Xiaomi',  1, '2026-03-01 09:30:00'),
  (4, 4, '老人来电王 F1',    'Nokia',   0, '2026-01-20 14:00:00'),
  (5, 5, '快充数据线',       'Anker',   1, '2026-02-15 16:20:00'),
  (6, 6, 'MacBook Air M3',  'Apple',   1, '2026-03-12 08:00:00'),
  (7, 3, '停售样机 X',       'Apple',   0, '2026-01-01 00:00:00');

-- SKU：价格/库存在这一层；SPU 1 带 3 个规格，其余 1~2 个
INSERT INTO sku (spu_id, sku_code, price, stock, spec_json, status) VALUES
  (1, 'IP15P-256-BLK', 7999.00, 100, JSON_OBJECT('容量','256G','颜色','黑色'), 1),
  (1, 'IP15P-256-WHT', 7999.00,  80, JSON_OBJECT('容量','256G','颜色','白色'), 1),
  (1, 'IP15P-512-BLK', 9999.00,  30, JSON_OBJECT('容量','512G','颜色','黑色'), 1),
  (2, 'MI14-256-GRN',  4299.00, 200, JSON_OBJECT('容量','256G','颜色','绿色'), 1),
  (2, 'MI14-512-BLK',  4799.00, 150, JSON_OBJECT('容量','512G','颜色','黑色'), 1),
  (3, 'RMN13-128-BLU', 1199.00, 500, JSON_OBJECT('容量','128G','颜色','蓝色'), 1),
  (4, 'F1-BLK',         299.00,  10, JSON_OBJECT('颜色','黑色'),               1),
  (5, 'CABLE-1M',        59.00, 999, JSON_OBJECT('长度','1m'),                 1),
  (6, 'MBA-M3-512',   10999.00,  20, JSON_OBJECT('容量','512G','颜色','银色'), 1);

SELECT 'category' AS tbl, COUNT(*) AS cnt FROM category
UNION ALL SELECT 'spu', COUNT(*) FROM spu
UNION ALL SELECT 'sku', COUNT(*) FROM sku;
