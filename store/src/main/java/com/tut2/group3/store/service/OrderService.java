package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.pojo.Result;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrderService {
    /**
     * Place a new order
     * - Validates inventory
     * - Reserves stock
     * - Processes payment
     * - Sends delivery request
     * - Confirms reservation
     */
    Result orderPlace(OrderCreateRequestDTO orderCreateRequestDTO);

    /**
     * Cancel an order before delivery request is sent
     * - Releases reserved stock
     * - Processes refund
     * - Sends refund notification email
     */
    Result cancelOrder(Long orderId, Long userId);

    /**
     * Get order by ID
     */
    Result getOrderById(Long orderId);

    /**
     * Get all orders for a user
     */
    Result getOrdersByUserId(Long userId);

    /**
     * Update order delivery status based on DeliveryCo status update
     * - Updates order status field
     * - Handles special cases (DELIVERED, LOST)
     *
     * @param orderId Order ID
     * @param deliveryStatus New delivery status from DeliveryCo
     * @param message Additional message about status update
     */
    void updateDeliveryStatus(Long orderId, String deliveryStatus, String message);

    /**
     * Cancel an order due to service timeout
     * - Bypasses normal cancellation restrictions
     * - Releases reserved stock
     * - Processes refund if payment was made
     * - Sends timeout notification email
     *
     * @param orderId Order ID
     * @param reason Timeout reason
     */
    void cancelOrderDueToTimeout(Long orderId, String reason);
}
