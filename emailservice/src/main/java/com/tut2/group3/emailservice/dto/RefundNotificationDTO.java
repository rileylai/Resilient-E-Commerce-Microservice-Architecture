package com.tut2.group3.emailservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refund notification DTO
 * Received from Store service when a refund is processed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundNotificationDTO {

    /**
     * Order ID from store service
     */
    private Long orderId;

    /**
     * Customer email for notification
     */
    private String customerEmail;

    /**
     * Refund amount
     */
    private Double amount;

    /**
     * Timestamp when refund was processed
     */
    private LocalDateTime timestamp;

    /**
     * Reason for refund (optional)
     */
    private String reason;
}
