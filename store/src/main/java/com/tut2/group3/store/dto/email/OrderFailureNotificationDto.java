package com.tut2.group3.store.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Order failure notification DTO
 * Sent to EmailService when order processing fails
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFailureNotificationDto {

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

