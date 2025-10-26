package com.tut2.group3.store.dto.deliveryco;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Delivery request DTO
 * Sent to DeliveryCo service when an order payment is successful
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRequestDto {

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
     * List of warehouse IDs where products are located
     */
    private List<Long> warehouseIds;

    /**
     * List of products to be delivered
     */
    private List<ProductInfo> products;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProductInfo {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Long warehouseId;
    }
}

