package com.study.mall.product.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private final String code;
    private final String message;
    private final T data;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    /** 有数据的成功响应 */
    public static <T> Result<T> ok(T data) {
        return new Result<>("SUCCESS", "ok", data);
    }

    /** 无数据的成功响应（配合 204 No Content 使用） */
    public static Result<Void> ok() {
        return new Result<>("SUCCESS", "ok", null);
    }

    /** 业务失败响应，code 为语义字符串，无数据 */
    public static Result<Void> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    /** 业务失败响应，附带结构化错误详情（如字段校验错误 Map） */
    public static <T> Result<T> fail(String code, String message, T errors) {
        return new Result<>(code, message, errors);
    }
}
