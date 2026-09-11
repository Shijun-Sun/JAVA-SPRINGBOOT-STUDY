package com.study.mall.product.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品响应视图对象，面向客户端。
 * 不含 updatedAt 等内部运维字段，不加校验注解。
 */
public record ProductVO(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String category,
        LocalDateTime createdAt
) {}
