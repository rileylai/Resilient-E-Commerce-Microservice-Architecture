package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Place a new order
     * 
     * Process flow:
     * 1. Validate inventory with warehouse
     * 2. Reserve stock
     * 3. Process payment through bank
     * 4. Send delivery request to DeliveryCo
     * 5. Confirm stock reservation
     * 
     * @param orderCreateRequestDTO Order creation request containing items and user ID
     * @return Result with order details or error message
     */
    @PostMapping("/place")
    public Result orderPlace(@RequestBody OrderCreateRequestDTO orderCreateRequestDTO) {
        log.info("Received order placement request from user: {}", orderCreateRequestDTO.getUserId());
        try {
            Result result = orderService.orderPlace(orderCreateRequestDTO);
            log.info("Order placement request processed: {}", result.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Error processing order placement: {}", e.getMessage(), e);
            return Result.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Cancel an order
     * 
     * Can only cancel orders before delivery request is sent.
     * Process flow:
     * 1. Release reserved stock
     * 2. Process refund if payment was made
     * 3. Send refund notification email
     * 
     * @param orderId Order ID to cancel
     * @param userId User ID making the cancellation request
     * @return Result with cancellation status
     */
    @PostMapping("/cancel/{orderId}")
    public Result cancelOrder(@PathVariable Long orderId, @RequestParam Long userId) {
        log.info("Received order cancellation request - Order: {}, User: {}", orderId, userId);
        try {
            Result result = orderService.cancelOrder(orderId, userId);
            log.info("Order cancellation request processed: {}", result.getMessage());
            return result;
        } catch (Exception e) {
            log.error("Error processing order cancellation: {}", e.getMessage(), e);
            return Result.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get order by ID
     * 
     * @param orderId Order ID
     * @return Result with order details
     */
    @GetMapping("/{orderId}")
    public Result getOrderById(@PathVariable Long orderId) {
        log.info("Received get order request - Order: {}", orderId);
        try {
            Result result = orderService.getOrderById(orderId);
            return result;
        } catch (Exception e) {
            log.error("Error getting order: {}", e.getMessage(), e);
            return Result.error(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get all orders for a user
     * 
     * @param userId User ID
     * @return Result with list of orders
     */
    @GetMapping("/user/{userId}")
    public Result getOrdersByUserId(@PathVariable Long userId) {
        log.info("Received get orders request - User: {}", userId);
        try {
            Result result = orderService.getOrdersByUserId(userId);
            return result;
        } catch (Exception e) {
            log.error("Error getting orders: {}", e.getMessage(), e);
            return Result.error(500, "Internal server error: " + e.getMessage());
        }
    }
}
