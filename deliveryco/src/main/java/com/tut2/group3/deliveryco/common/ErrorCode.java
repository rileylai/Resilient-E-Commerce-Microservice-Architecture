package com.tut2.group3.deliveryco.common;

import lombok.Getter;

/**
 * Error Code Enum
 *
 * Standardized error codes for delivery operations.
 */
@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    NOT_FOUND(404, "Not found"),
    CONFLICT(409, "Conflict"),
    INTERNAL_ERROR(500, "Internal server error"),

    DELIVERY_NOT_FOUND(701, "Delivery not found"),
    DELIVERY_ALREADY_COMPLETED(702, "Delivery already completed or lost"),
    INVALID_STATUS_TRANSITION(703, "Invalid delivery status transition"),
    ORDER_NOT_FOUND(704, "Order not found"),
    CUSTOMER_NOT_FOUND(705, "Customer not found"),
    INVALID_DELIVERY_REQUEST(706, "Invalid delivery request"),
    DELIVERY_CREATION_FAILED(707, "Failed to create delivery"),
    STATUS_UPDATE_FAILED(708, "Failed to update delivery status"),
    MESSAGE_SEND_FAILED(709, "Failed to send notification message"),
    PACKAGE_LOST(710, "Package has been lost during delivery");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
