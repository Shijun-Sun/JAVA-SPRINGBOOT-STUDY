package com.study.mall.product.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表页视图对象，只含摘要字段。
 * 相比 {@link ProductVO} 刻意不含 sku：列表页不展示 SKU，SKU 属于详情页字段。
 * 不加校验注解，转换逻辑统一在 Service 层。
 */
public record ProductSummaryVO(
        Long id,
        String name,
        BigDecimal price,
        String category,
        LocalDateTime createdAt
) {}
