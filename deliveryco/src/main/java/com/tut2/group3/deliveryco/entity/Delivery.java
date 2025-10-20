package com.tut2.group3.deliveryco.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Delivery entity representing a delivery record in the database
 * Tracks the entire delivery process from warehouse pickup to customer delivery
 */
@Data
@NoArgsConstructor
@TableName("deliveries")
public class Delivery {

    /**
     * Unique identifier for the delivery
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Associated order ID from the store service
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * Customer ID who placed the order
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * Comma-separated list of warehouse IDs where items are located
     * Example: "1,3" means items are in warehouses 1 and 3
     */
    @TableField("warehouse_ids")
    private String warehouseIds;

    /**
     * Current status of the delivery
     * Follows the state machine: REQUEST_RECEIVED -> PICKED_UP -> IN_TRANSIT -> DELIVERED/LOST
     */
    @TableField("status")
    private DeliveryStatus status;

    /**
     * Timestamp when the delivery record was created
     * Auto-filled by MyBatis-Plus on insert
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the delivery record was last updated
     * Auto-filled by MyBatis-Plus on insert and update
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}