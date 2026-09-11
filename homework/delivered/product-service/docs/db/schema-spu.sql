-- SPU 表：商品的抽象（详情页标题那一层），价格/库存不在这里，在 sku
-- category_id 为逻辑外键，不加物理 FOREIGN KEY（理由见 商品域建模说明.md）
CREATE TABLE `spu` (
    `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`  BIGINT UNSIGNED  NOT NULL                COMMENT '分类 id，逻辑外键指向 category.id',
    `name`         VARCHAR(100)     NOT NULL                COMMENT '商品名称（SPU 标题）',
    `brand`        VARCHAR(100)     NOT NULL                COMMENT '品牌名称',
    `description`  VARCHAR(1000)    DEFAULT NULL            COMMENT '商品描述',
    `status`       TINYINT          NOT NULL DEFAULT 1      COMMENT '状态：1-上架 0-下架',
    `created_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    `created_by`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '创建人 id',
    `updated_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '更新人 id',
    `deleted_at`   DATETIME         DEFAULT NULL            COMMENT '软删除时间，NULL 表示未删除',
    `version`      BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    -- 服务 Q2：SELECT ... WHERE category_id = ? AND status = 1 ORDER BY created_at DESC
    -- category_id、status 两个等值列在前作定位，created_at 放最后：
    -- 前两列等值锁定后，该段内 created_at 天然有序，正/倒序都能走索引避免 filesort
    KEY `idx_category_status_created` (`category_id`, `status`, `created_at`)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '商品表（SPU 标准产品单元）';
