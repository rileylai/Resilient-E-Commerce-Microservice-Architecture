package com.tut2.group3.store.dto.email;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Refund notification DTO
 * Sent to EmailService when a refund is processed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundNotificationDto {

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

