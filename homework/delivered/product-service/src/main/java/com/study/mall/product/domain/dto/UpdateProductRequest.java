package com.study.mall.product.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新商品的入参 DTO。
 * SKU 不在更新范围内，不允许客户端修改。
 */
public record UpdateProductRequest(

        @NotBlank(message = "商品名称不能为空")
        @Size(min = 1, max = 100, message = "商品名称长度须在 1～100 个字符之间")
        String name,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        @NotBlank(message = "分类不能为空")
        String category

) {}
