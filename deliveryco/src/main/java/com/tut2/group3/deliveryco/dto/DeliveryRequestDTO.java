package com.tut2.group3.deliveryco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Delivery request DTO
 * Consumed from delivery.request.queue
 * Sent by store-service when an order payment is successful
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequestDTO {

    /**
     * Order ID from store service
     */
    private Long orderId;

    /**
     * Customer ID who placed the order
     */
    private Long customerId;

    /**
     * Customer email for notification purposes
     */
    private String customerEmail;

    /**
     * List of warehouse where products are located
     */
    private List<Long> warehouseIds;

    /**
     * List of products to be delivered
     */
    private List<ProductInfo> products;
}