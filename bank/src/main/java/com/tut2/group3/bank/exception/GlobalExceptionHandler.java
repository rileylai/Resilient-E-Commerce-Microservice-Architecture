package com.tut2.group3.bank.exception;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tut2.group3.bank.common.Result;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(400,
                StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "Invalid parameter");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // BusinessException 可以自带错误码
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleGenericException(Exception e) {
        return Result.error(500,
                StringUtils.hasLength(e.getMessage()) ? e.getMessage() : "Internal server error");
    }
}
