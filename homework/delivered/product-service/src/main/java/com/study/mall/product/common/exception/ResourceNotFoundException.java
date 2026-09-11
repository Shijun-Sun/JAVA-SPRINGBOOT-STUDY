package com.study.mall.product.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(Long id, String resourceName) {
        super("RESOURCE_NOT_FOUND",resourceName + " 不存在： " + id, HttpStatus.NOT_FOUND);
    }
}
