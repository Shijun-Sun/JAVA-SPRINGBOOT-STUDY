package com.study.mall.product.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 创建商品的入参 DTO。
 * 所有字段在 Controller 边界做校验，不流入 Repository。
 */
public record CreateProductRequest(

        @NotBlank(message = "商品名称不能为空")
        @Size(min = 1, max = 100, message = "商品名称长度须在 1～100 个字符之间")
        String name,

        @NotBlank(message = "SKU 不能为空")
        @Pattern(regexp = "^[A-Z0-9\\-]{1,50}$",
                message = "SKU 只允许大写字母、数字和连字符，最多 50 个字符")
        String sku,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        @NotBlank(message = "分类不能为空")
        String category

) {}
