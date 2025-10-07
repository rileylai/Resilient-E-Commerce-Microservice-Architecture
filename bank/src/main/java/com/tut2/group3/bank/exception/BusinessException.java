package com.tut2.group3.bank.exception;

import com.tut2.group3.bank.common.ErrorCode;

public class BusinessException extends RuntimeException {

    private final int code;

    // constructor using ErrorCode
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    // constructor with custom message
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    // legacy constructors (optional, keep for flexibility)
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // getter
    public int getCode() {
        return code;
    }
}