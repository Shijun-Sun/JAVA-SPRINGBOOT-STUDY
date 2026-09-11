package com.study.mall.product.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidBusinessArgumentException extends BusinessException {

    public InvalidBusinessArgumentException() {
        super("SYNTACTICALLY_VALID_BUT_SEMANTICALLY_INVALID", "格式合法但语义非法", HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
