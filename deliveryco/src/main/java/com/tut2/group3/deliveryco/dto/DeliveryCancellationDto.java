package com.tut2.group3.deliveryco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery cancellation DTO
 * Received from Store service when an order is cancelled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryCancellationDto {

    /**
     * Order ID from store service
     */
    private Long orderId;

    /**
     * Cancellation reason
     */
    private String reason;

    /**
     * Timestamp of cancellation
     */
    private LocalDateTime timestamp;

    /**
     * Customer email for notification purposes
     */
    private String customerEmail;
}
