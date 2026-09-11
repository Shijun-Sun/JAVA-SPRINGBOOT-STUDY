package com.study.mall.product.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    BusinessException(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.httpStatus = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
