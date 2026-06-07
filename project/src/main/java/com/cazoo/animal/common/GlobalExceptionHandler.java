package com.cazoo.animal.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException ex) {
        return Result.error(ex.getCode().getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("参数校验失败");
        return Result.error(ResultCode.VALIDATION_FAILED.getCode(), msg);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return Result.error(ResultCode.FILE_TOO_LARGE);
    }

    /**
     * Spring Security 6.5 method security 抛 AuthorizationDeniedException(被 @PreAuthorize 拒绝),
     * 不会进过滤器链,需要 GlobalExceptionHandler 兜住,返回统一的 4003 JSON。
     * (注意:运行时抛的可能是 org.springframework.security.authorization.AuthorizationDeniedException,
     *  也可以是 org.springframework.security.access.AccessDeniedException —— 都接住)
     */
    @ExceptionHandler({AuthorizationDeniedException.class, org.springframework.security.access.AccessDeniedException.class})
    public Result<?> handleAccessDenied(RuntimeException ex) {
        return Result.error(ResultCode.ACCESS_DENIED);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknown(Exception ex) {
        log.error("未捕获异常", ex);
        return Result.error(ResultCode.INTERNAL_ERROR);
    }
}
