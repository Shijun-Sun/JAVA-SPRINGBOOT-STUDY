package com.study.mall.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品数据库行映射对象。
 * 只在 Repository 层和 Service 层内部流转，不出现在 Controller 返回值或请求参数中。
 */
public record ProductEntity(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
