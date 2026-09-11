package com.study.mall.product.common.exception;

import org.springframework.http.HttpStatus;

/**
 * SKU 已存在时抛出，对应 HTTP 409 Conflict。
 */
public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String sku) {
        super("PRODUCT_SKU_CONFLICT", "SKU 已存在：" + sku, HttpStatus.CONFLICT);
    }
}
