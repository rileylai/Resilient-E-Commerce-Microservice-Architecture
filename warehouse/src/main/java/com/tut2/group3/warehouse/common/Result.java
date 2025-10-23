package com.tut2.group3.warehouse.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    // success (no data)
    public static <T> Result<T> success() {
        return new Result<>(200, "Success", null);
    }

    // success (with data)
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "Success", data);
    }

    // success with custom message
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // use predefined error code
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    // custom error message with error code
    public static <T> Result<T> error(ErrorCode errorCode, String customMessage) {
        return new Result<>(errorCode.getCode(), customMessage, null);
    }

    // error with data
    public static <T> Result<T> error(ErrorCode errorCode, String customMessage, T data) {
        return new Result<>(errorCode.getCode(), customMessage, data);
    }
}
