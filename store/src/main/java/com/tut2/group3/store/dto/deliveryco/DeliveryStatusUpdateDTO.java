package com.tut2.group3.store.dto.deliveryco;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery status update DTO
 * Received from DeliveryCo service via RabbitMQ
 * Contains delivery status changes to update order status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusUpdateDTO {
    
    /**
     * Order ID from store service
     */
    private Long orderId;
    
    /**
     * New delivery status
     * Possible values: REQUEST_RECEIVED, PICKED_UP, IN_TRANSIT, DELIVERED, LOST
     */
    private String newStatus;
    
    /**
     * Customer email for notification
     */
    private String customerEmail;
    
    /**
     * Timestamp when status was updated
     */
    private LocalDateTime timestamp;
    
    /**
     * Additional message or details about the status update
     */
    private String message;
}

