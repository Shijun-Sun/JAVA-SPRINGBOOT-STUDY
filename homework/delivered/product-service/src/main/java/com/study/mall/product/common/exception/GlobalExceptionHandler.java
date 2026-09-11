package com.study.mall.product.common.exception;

import com.study.mall.product.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @Validated @RequestBody 校验失败。
     * 收集所有字段错误，放入 data，整体返回 400。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "校验失败" : fe.getDefaultMessage(),
                        (a, b) -> a  // 同一字段多条错误时取第一条
                ));
        return Result.fail("VALIDATION_FAILED", "入参校验失败", errors);
    }

    /**
     * 处理 @Validated @RequestParam / @PathVariable 校验失败（类级别 @Validated 触发）。
     * MethodArgumentNotValidException 处理 @RequestBody；
     * ConstraintViolationException 处理 @RequestParam 和 @PathVariable。
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> {
                            // 路径形如 methodName.paramName，只取最后一段
                            String path = v.getPropertyPath().toString();
                            int dot = path.lastIndexOf('.');
                            return dot >= 0 ? path.substring(dot + 1) : path;
                        },
                        v -> v.getMessage() == null ? "校验失败" : v.getMessage(),
                        (a, b) -> a
                ));
        return Result.fail("VALIDATION_FAILED", "入参校验失败", errors);
    }

    /**
     * 处理 JSON 反序列化失败（如 price 传了字符串、请求体不是合法 JSON）。
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable() {
        return Result.fail("INVALID_FORMAT", "请求体格式错误，请检查 JSON 格式和字段类型");
    }

    /**
     * 处理所有 BusinessException 及其子类（ResourceNotFoundException、ResourceConflictException 等）。
     * HTTP 状态码由异常自身携带，不在这里硬编码。
     */
    @ExceptionHandler(BusinessException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        return org.springframework.http.ResponseEntity
                .status(ex.getHttpStatus())
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * 兜底处理器：捕获所有未预期异常，打完整 ERROR 日志，不把 ex.getMessage() 暴露给客户端。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpected(Exception ex,
            jakarta.servlet.http.HttpServletRequest request) {
        log.error("[UNHANDLED] {} {}", request.getMethod(), request.getRequestURI(), ex);
        return Result.fail("INTERNAL_ERROR", "服务内部错误，请稍后重试");
    }
}
