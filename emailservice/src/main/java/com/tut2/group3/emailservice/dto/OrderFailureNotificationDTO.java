package com.tut2.group3.emailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Order failure notification DTO
 * Received from Store service when order processing fails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderFailureNotificationDTO {

    /**
     * Order ID from store service
     */
    private Long orderId;

    /**
     * Customer email for notification
     */
    private String customerEmail;

    /**
     * Failure reason
     */
    private String reason;

    /**
     * Timestamp when failure occurred
     */
    private LocalDateTime timestamp;

    /**
     * Additional error details (optional)
     */
    private String errorDetails;
}
