-- 分类表：邻接表模型（id + parent_id）表达分类树
-- 查询索引在第 03 章按高频查询设计：见表内 idx_parent_sort（服务分类页 Q1）
CREATE TABLE `category` (
    `id`          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id`   BIGINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT '父分类 id，0 表示顶级分类',
    `name`        VARCHAR(50)       NOT NULL                COMMENT '分类名称',
    `level`       TINYINT UNSIGNED  NOT NULL DEFAULT 1      COMMENT '分类层级：1-一级 2-二级 3-三级',
    `sort`        INT               NOT NULL DEFAULT 100    COMMENT '同级排序，值越小越靠前，间隔 100 便于后续插入',
    `created_at`  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP                     COMMENT '创建时间',
    `created_by`  BIGINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT '创建人 id',
    `updated_at`  DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `updated_by`  BIGINT UNSIGNED   NOT NULL DEFAULT 0      COMMENT '更新人 id',
    `deleted_at`  DATETIME          DEFAULT NULL            COMMENT '软删除时间，NULL 表示未删除',
    `version`     BIGINT UNSIGNED   NOT NULL DEFAULT 1      COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    -- 服务 Q1：SELECT ... WHERE parent_id = ? ORDER BY sort
    -- parent_id 等值定位在前，sort 进索引前缀提供有序读取，避免 filesort
    KEY `idx_parent_sort` (`parent_id`, `sort`)
)   ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '商品分类表（邻接表树模型）';
