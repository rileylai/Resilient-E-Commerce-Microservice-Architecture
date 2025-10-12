package com.tut2.group3.deliveryco.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product information DTO
 * Contains basic product details for delivery
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfo {

    /**
     * Product ID
     */
    private Long productId;

    /**
     * Product name
     */
    private String productName;

    /**
     * Quantity ordered
     */
    private Integer quantity;

    /**
     * Warehouse ID where the product is stored
     */
    private Long warehouseId;
}