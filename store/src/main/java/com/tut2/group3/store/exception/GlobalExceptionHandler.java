package com.tut2.group3.store.exception;

import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOtherException(Exception ex) {
        return Result.error("server error：" + ex.getMessage());
    }
}
