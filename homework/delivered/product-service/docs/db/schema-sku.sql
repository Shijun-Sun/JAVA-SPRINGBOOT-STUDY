-- SKU 表：可下单、可扣库存的最小单位，价格与库存落在这里
-- spu_id 为逻辑外键，不加物理 FOREIGN KEY（理由见 商品域建模说明.md）
CREATE TABLE `sku` (
    `id`           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '主键',
    `spu_id`       BIGINT UNSIGNED  NOT NULL                COMMENT 'SPU id，逻辑外键指向 spu.id',
    `sku_code`     VARCHAR(64)      NOT NULL                COMMENT 'SKU 编码，业务唯一',
    `price`        DECIMAL(10, 2)   NOT NULL                COMMENT '销售价格，单位元',
    `stock`        INT UNSIGNED     NOT NULL DEFAULT 0      COMMENT '库存数量',
    `spec_json`    JSON             DEFAULT NULL            COMMENT '销售属性，如 {"容量":"256G","颜色":"黑色"}',
    `status`       TINYINT          NOT NULL DEFAULT 1      COMMENT '状态：1-上架 0-下架',
    `created_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    `created_by`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '创建人 id',
    `updated_at`   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`   BIGINT UNSIGNED  NOT NULL DEFAULT 0      COMMENT '更新人 id',
    `deleted_at`   DATETIME         DEFAULT NULL            COMMENT '软删除时间，NULL 表示未删除',
    `version`      BIGINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sku_code` (`sku_code`),
    -- 服务 Q3：SELECT * FROM sku WHERE spu_id = ?（商品详情页取某 SPU 下全部 SKU）
    -- 单列即可：spu_id 是唯一过滤条件，无附带排序需求；查 * 必然回表，不做覆盖
    KEY `idx_spu_id` (`spu_id`)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '库存单位表（SKU 库存量单位）';
