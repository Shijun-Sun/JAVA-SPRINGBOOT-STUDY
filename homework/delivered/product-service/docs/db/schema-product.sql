-- 商品单品过渡表：把 M01 的 ProductEntity 原样落成一张规范 MySQL 表
-- 对应讲义 6.1，作为第 10 章内存存储换 MySQL 的平滑过渡模型
CREATE TABLE `product` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100)     NOT NULL                COMMENT '商品名称',
    `sku`         VARCHAR(64)      NOT NULL                COMMENT 'SKU 编码，业务唯一',
    `price`       DECIMAL(10, 2)   NOT NULL                COMMENT '商品价格，单位元',
    `category`    VARCHAR(50)      NOT NULL                COMMENT '分类名称（过渡字段，后续改为 category_id）',
    `status`      TINYINT          NOT NULL DEFAULT 1      COMMENT '状态：1-上架 0-下架',
    `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    `created_by`  BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '创建人 id',
    `updated_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`  BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '更新人 id',
    `deleted_at`  DATETIME         DEFAULT NULL            COMMENT '软删除时间，NULL 表示未删除',
    `version`     BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku` (`sku`)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '商品表（单品过渡模型）';
