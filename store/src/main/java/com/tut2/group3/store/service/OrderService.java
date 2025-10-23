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
    Result orderPlace(@RequestBody OrderCreateRequestDTO orderCreateRequestDTO);

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
}
