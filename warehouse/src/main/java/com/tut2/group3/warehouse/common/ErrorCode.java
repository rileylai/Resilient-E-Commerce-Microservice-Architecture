package com.tut2.group3.warehouse.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    NOT_FOUND(404, "Not found"),
    CONFLICT(409, "Conflict"),
    INTERNAL_ERROR(500, "Internal server error"),

    // Warehouse specific error codes
    INSUFFICIENT_STOCK(601, "Insufficient stock available"),
    STOCK_RESERVATION_FAILED(602, "Stock reservation failed"),
    RESERVATION_NOT_FOUND(603, "Reservation not found"),
    WAREHOUSE_NOT_FOUND(604, "Warehouse not found"),
    PRODUCT_NOT_FOUND(605, "Product not found"),
    STOCK_UPDATE_FAILED(606, "Stock update failed"),
    INVALID_QUANTITY(607, "Invalid quantity");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
