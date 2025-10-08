package com.tut2.group3.bank.exception;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // invalid arguments (e.g., invalid enum or parameter)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = StringUtils.hasLength(e.getMessage())
                ? e.getMessage()
                : ErrorCode.BAD_REQUEST.getMessage();
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    // business exception (e.g., debit/credit fail)
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    // all other unexpected exceptions
    @ExceptionHandler(Exception.class)
    public Result<?> handleGenericException(Exception e) {
        String message = StringUtils.hasLength(e.getMessage())
                ? e.getMessage()
                : ErrorCode.INTERNAL_ERROR.getMessage();
        return Result.error(ErrorCode.INTERNAL_ERROR, message);
    }
}