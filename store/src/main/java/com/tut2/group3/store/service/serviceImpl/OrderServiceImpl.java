package com.tut2.group3.store.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tut2.group3.store.client.BankClient;
import com.tut2.group3.store.client.WarehouseClient;
import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.TransactionDto;
import com.tut2.group3.store.dto.deliveryco.DeliveryCancellationDto;
import com.tut2.group3.store.dto.deliveryco.DeliveryRequestDto;
import com.tut2.group3.store.dto.email.OrderFailureNotificationDto;
import com.tut2.group3.store.dto.email.RefundNotificationDto;
import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.dto.order.OrderItemDetailDto;
import com.tut2.group3.store.dto.order.OrderItemRequestDTO;
import com.tut2.group3.store.dto.order.OrderResponseDto;
import com.tut2.group3.store.dto.warehouse.*;
import com.tut2.group3.store.exception.BusinessException;
import com.tut2.group3.store.mapper.OrderItemMapper;
import com.tut2.group3.store.mapper.OrderMapper;
import com.tut2.group3.store.mapper.UserMapper;
import com.tut2.group3.store.pojo.Order;
import com.tut2.group3.store.pojo.OrderItem;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.pojo.User;
import com.tut2.group3.store.service.MessagePublisher;
import com.tut2.group3.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.web.client.RestClientException;
import feign.FeignException;
import feign.RetryableException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final WarehouseClient warehouseClient;
    private final BankClient bankClient;
    private final MessagePublisher messagePublisher;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @Override
    public Result orderPlace(OrderCreateRequestDTO orderCreateRequestDTO) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Starting order placement process for user: {}", orderCreateRequestDTO.getUserId());
        
        Order order = null;
        String reservationId = null;
        boolean paymentProcessed = false;
        User user = null;
        try {
            user = userMapper.selectById(orderCreateRequestDTO.getUserId());
            if (user == null) {
                log.error("User not found: {}", orderCreateRequestDTO.getUserId());
                return Result.error(404, "User not found");
            }

            // Step 1: Validate order with warehouse
            log.info("Step 1: Validating order with warehouse...");
            OrderValidationRequest validationRequest = buildValidationRequest(orderCreateRequestDTO);
            Result<OrderValidationResponse> validationResult = warehouseClient.validateOrder(validationRequest);
            
            if (validationResult.getCode() != 200 || !validationResult.getData().getValid()) {
                String errorMessage = validationResult.getData() != null ? 
                    validationResult.getData().getMessage() : "Inventory validation failed";
                log.error("Inventory validation failed: {}", errorMessage);
                sendOrderFailureNotification(null, user.getEmail(), "Insufficient inventory", errorMessage);
                return Result.error(400, errorMessage);
            }
            log.info("Inventory validation successful");

            // Delay for testing cancel functionality
            log.info("Waiting 3 seconds before creating order...");
            Thread.sleep(3000);

            // After validation, create order in the database
            log.info("Step 2: Creating order in database...");
            OrderResponseDto orderResponse = createOrderTransaction(orderCreateRequestDTO, user);
            order = orderMapper.selectById(orderResponse.getOrderId());
            log.info("Order created with ID: {}", order.getId());
            log.info("Order Status: PENDING_VALIDATION (immediately visible for queries and cancel)");

            // Delay for testing cancel functionality
            log.info("Waiting 3 seconds before reserving stock...");
            Thread.sleep(3000);

            // Check if order was cancelled during the delay
            order = orderMapper.selectById(order.getId());
            if ("CANCELLED".equals(order.getStatus())) {
                log.info("Order {} was cancelled by user. Stopping order placement (no rollback needed, cancel method already handled it).", order.getId());
                return Result.success("Order was cancelled during processing.", null);
            }

            // Step 3: Reserve stock for each item with intelligent warehouse allocation
            reservationId = null;
            for (OrderItemRequestDTO item : orderCreateRequestDTO.getItems()) {
                // Check stock availability and get intelligent warehouse allocation
                CheckAvailabilityRequest availabilityRequest = new CheckAvailabilityRequest(
                    item.getProductId(),
                    item.getQuantity()
                );

                Result<StockAvailabilityResponse> availabilityResult = warehouseClient.checkAvailability(availabilityRequest);
                if (availabilityResult.getCode() != 200 || availabilityResult.getData() == null || !availabilityResult.getData().getAvailable()) {
                    log.error("Stock not available for productId {}: {}", item.getProductId(),
                        availabilityResult.getData() != null ? availabilityResult.getData().getTotalAvailableQuantity() : 0);
                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user.getEmail(), "Insufficient inventory",
                        "Stock not available for product " + item.getProductId());
                    return Result.error(400, "Insufficient inventory for productId " + item.getProductId());
                }

                StockAvailabilityResponse availability = availabilityResult.getData();

                // Build warehouse allocation from availability response (intelligent allocation)
                List<WarehouseAllocation> warehouseAllocations = availability.getWarehouses().stream()
                    .filter(wh -> wh.getAllocatedQuantity() != null && wh.getAllocatedQuantity() > 0)
                    .map(wh -> new WarehouseAllocation(wh.getWarehouseId(), wh.getAllocatedQuantity()))
                    .collect(Collectors.toList());

                if (warehouseAllocations.isEmpty()) {
                    log.error("No warehouse allocations available for productId {}", item.getProductId());
                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user.getEmail(), "Warehouse allocation failed",
                        "No warehouse can fulfill product " + item.getProductId());
                    return Result.error(400, "Warehouse allocation failed for productId " + item.getProductId());
                }

                log.info("Intelligent warehouse allocation for product {}: {} warehouse(s)",
                    item.getProductId(), warehouseAllocations.size());

                ReserveStockRequest reserveRequest = new ReserveStockRequest(
                    String.valueOf(order.getId()),
                    item.getProductId(),
                    item.getQuantity(),
                    warehouseAllocations
                );
                Result<StockReservationResponse> reserveResult = warehouseClient.reserveStock(reserveRequest);
                if (reserveResult.getCode() != 200 || reserveResult.getData() == null) {
                    log.error("Failed to reserve stock for productId {}: {}", item.getProductId(), reserveResult.getMessage());
                    // Rollback: set FAILED, send notify, return error
                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user.getEmail(), "Stock reservation failed",
                        "Failed to reserve stock for product " + item.getProductId());
                    return Result.error(400, "Failed to reserve stock for productId " + item.getProductId());
                }
                // use the first reservationId (for order-level tracking)
                if (reservationId == null) {
                    reservationId = reserveResult.getData().getReservationId();
                }
            }
            // update order with reservationId and status
            order.setReservationId(reservationId);
            order.setStatus("PENDING_PAYMENT");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("Stock reserved with reservation ID: {}", reservationId);
            log.info("Order Status: PENDING_VALIDATION -> PENDING_PAYMENT (immediately visible for queries and cancel)");

            // Delay for testing cancel functionality
            log.info("Waiting 3 seconds before processing payment...");
            Thread.sleep(3000);

            // Check if order was cancelled during the delay
            order = orderMapper.selectById(order.getId());
            if ("CANCELLED".equals(order.getStatus())) {
                log.info("Order {} was cancelled by user. Stopping order placement (no rollback needed, cancel method already handled it).", order.getId());
                return Result.success("Order was cancelled during processing.", null);
            }

            // Step 5: Process payment and update status (Independent transaction - immediately visible)
            log.info("Step 5: Processing payment through bank...");
            Result<TransactionDto> paymentResult = processPaymentAndUpdateStatus(order.getId(), user);
            
            if (paymentResult.getCode() != 200) {
                log.error("Payment failed: {}", paymentResult.getMessage());
                // Release reserved stock
                releaseReservedStock(String.valueOf(order.getId()), reservationId, "Payment failed");
                updateOrderStatusImmediate(order.getId(), "FAILED");
                log.error("Order Status: PENDING_PAYMENT -> FAILED (Reason: Payment failed)");
                sendOrderFailureNotification(order.getId(), user.getEmail(), "Payment failed", paymentResult.getMessage());
                return Result.error(400, "Payment failed: " + paymentResult.getMessage());
            }
            
            paymentProcessed = true;
            log.info("Payment successful. Transaction ID: {}", paymentResult.getData().getId());
            log.info("Order Status: PENDING_PAYMENT -> PAYMENT_SUCCESSFUL (immediately visible for queries and cancel)");

            // Delay for testing cancel functionality
            log.info("Waiting 5 seconds before sending delivery request...");
            Thread.sleep(5000);
            
            // Check if order was cancelled during the delay
            order = orderMapper.selectById(order.getId());
            if ("CANCELLED".equals(order.getStatus())) {
                log.info("Order {} was cancelled by user. Stopping order placement (no rollback needed, cancel method already handled it).", order.getId());
                return Result.success("Order was cancelled during processing.", null);
            }

            // Step 6: Send delivery request and update status (Independent transaction - immediately visible)
            log.info("Step 6: Sending delivery request to DeliveryCo...");
            sendDeliveryRequestAndUpdateStatus(order.getId(), orderCreateRequestDTO, user); // Pass null for stockAvailability
            log.info("Delivery request sent successfully");
            log.info("Order Status: PAYMENT_SUCCESSFUL -> DELIVERY_REQUESTED (immediately visible)");

            // Step 7: Confirm reservation with warehouse
            log.info("Step 7: Confirming reservation with warehouse...");
            confirmReservation(String.valueOf(order.getId()), reservationId);
            log.info("Reservation confirmed");
            log.info("Order status will be updated by DeliveryCo via message queue");

            log.info("════════════════════════════════════════════════════════════");
            log.info("Order placement completed successfully!");
            order = orderMapper.selectById(order.getId()); // Refresh order data
            log.info("Order ID: {}, Total: ${}, Status: {}", order.getId(), order.getTotalAmount(), order.getStatus());
            log.info("════════════════════════════════════════════════════════════");
            
            OrderResponseDto response = new OrderResponseDto();
            response.setOrderId(order.getId());
            response.setStatus(order.getStatus());
            response.setTotalAmount(order.getTotalAmount());
            response.setCreateTime(order.getCreateTime());
            
            return Result.success("Order placed successfully. Awaiting delivery.", response);

        } catch (DataAccessException dae) {
            log.error("Database Exception during order placement:", dae);
            if (order != null) {
                try {
                    if (paymentProcessed) processRefund(order, user);
                    if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "Database error");

                    // Send cancellation message to prevent delivery processing
                    sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Database error during order placement");

                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Database service unavailable", dae.getMessage());
                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            return Result.error(501, "Database service unavailable, please try again later!");
        } catch (AmqpConnectException mqEx) {
            log.error("RabbitMQ Exception during order placement:", mqEx);
            if (order != null) {
                try {
                    if (paymentProcessed) processRefund(order, user);
                    if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "RabbitMQ error");

                    // Send cancellation message to prevent delivery processing
                    sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Message queue error during order placement");

                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Message queue service unreachable", mqEx.getMessage());
                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            return Result.error(502, "Message queue exception, please try again later!");
        } catch (RestClientException remoteEx) {
            String remoteMsg = remoteEx.getMessage() != null ? remoteEx.getMessage().toLowerCase() : "";
            log.error("Remote Service Exception during order placement:", remoteEx);
            if (order != null) {
                try {
                    if (remoteMsg.contains("bank") || remoteMsg.contains("8084")) {
                        // Bank service unavailable
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "Bank service unreachable");

                        // Send cancellation message to prevent delivery processing
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Bank service unavailable during order placement");

                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Payment service temporarily unavailable", "Unable to process payment at this time");
                        return Result.error(503, "Payment service is currently unavailable. Please try again later.");

                    } else if (remoteMsg.contains("deliveryco")) {
                        // DeliveryCo service unavailable
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "DeliveryCo unreachable");

                        // Send cancellation message to prevent delivery processing when service restarts
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "DeliveryCo service unavailable during order placement");

                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Delivery service temporarily unavailable", refundMsg);
                        return Result.error(503, "Delivery service is currently unavailable. " + refundMsg);

                    } else if (remoteMsg.contains("warehouse")) {
                        // Warehouse service unavailable
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "Warehouse service error");

                        // Send cancellation message to prevent delivery processing
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Warehouse service error during order placement");

                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Warehouse service temporarily unavailable", refundMsg);
                        return Result.error(503, "Warehouse service is currently unavailable. " + refundMsg);

                    } else {
                        // Unknown external service error
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "External service error");

                        // Send cancellation message to prevent delivery processing
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "External service error during order placement");

                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Service temporarily unavailable", refundMsg);
                        return Result.error(503, "A required service is currently unavailable. " + refundMsg);
                    }
                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            return Result.error(503, "Service temporarily unavailable. Please try again later.");
        } catch (FeignException feignEx) {
            String feignMsg = feignEx.getMessage() != null ? feignEx.getMessage().toLowerCase() : "";
            int status = feignEx.status();
            log.error("Feign Client Exception during order placement (status: {}): {}", status, feignEx.getMessage());

            if (order != null) {
                try {
                    // Check which service based on request URL or message content
                    if (feignMsg.contains("8084") || feignMsg.contains("bank")) {
                        // Bank service error
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "Bank service error");
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Bank service error");
                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Payment service temporarily unavailable", "Unable to process payment at this time");
                        return Result.error(503, "Payment service is currently unavailable. Please try again later.");

                    } else if (feignMsg.contains("8083") || feignMsg.contains("deliveryco")) {
                        // DeliveryCo service error
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "DeliveryCo error");
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "DeliveryCo service error");
                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Delivery service temporarily unavailable", refundMsg);
                        return Result.error(503, "Delivery service is currently unavailable. " + refundMsg);

                    } else if (feignMsg.contains("8082") || feignMsg.contains("warehouse")) {
                        // Warehouse service error
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "Warehouse service error");
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Warehouse service error");
                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Warehouse service temporarily unavailable", refundMsg);
                        return Result.error(503, "Warehouse service is currently unavailable. " + refundMsg);

                    } else {
                        // Unknown service error
                        if (paymentProcessed) processRefund(order, user);
                        if (reservationId != null) releaseReservedStock(String.valueOf(order.getId()), reservationId, "External service error");
                        sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "External service error");
                        updateOrderStatusImmediate(order.getId(), "FAILED");
                        String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                        sendOrderFailureNotification(order.getId(), user != null ? user.getEmail() : "", "Service temporarily unavailable", refundMsg);
                        return Result.error(503, "A required service is currently unavailable. " + refundMsg);
                    }
                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            return Result.error(503, "Service temporarily unavailable. Please try again later.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Order placement interrupted: {}", e.getMessage());
            if (order != null) {
                try {
                    user = userMapper.selectById(orderCreateRequestDTO.getUserId());
                    if (paymentProcessed) {
                        processRefund(order, user);
                    }
                    if (reservationId != null) {
                        releaseReservedStock(String.valueOf(order.getId()), reservationId, "Order interrupted");
                    }

                    // Send cancellation message to prevent delivery processing
                    sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Order placement interrupted");

                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    sendOrderFailureNotification(order.getId(), user.getEmail(), "Order interrupted", e.getMessage());
                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }
            return Result.error(500, "Order placement interrupted");
            
        } catch (BusinessException e) {
            // If the order was cancelled, the cancel method already handled rollback
            if (e.getMessage() != null && e.getMessage().contains("cancelled")) {
                log.info("Order placement stopped because order was cancelled by user.");
                return Result.success("Order was cancelled during processing.", null);
            }
            // For other business exceptions, treat as regular error
            log.error("Business exception during order placement: {}", e.getMessage(), e);
            return Result.error(e.getCode() != null ? e.getCode() : 500, e.getMessage());
            
        } catch (Exception e) {
            log.error("════════════════════════════════════════════════════════════");
            log.error("Order placement failed with exception: {}", e.getMessage(), e);

            // Rollback actions
            if (order != null) {
                try {
                    user = userMapper.selectById(orderCreateRequestDTO.getUserId());

                    // If payment was processed, refund it
                    if (paymentProcessed) {
                        log.info("Rolling back payment...");
                        processRefund(order, user);
                    }

                    // If stock was reserved, release it
                    if (reservationId != null) {
                        log.info("Releasing reserved stock...");
                        releaseReservedStock(String.valueOf(order.getId()), reservationId, "Order processing failed");
                    }

                    // Send cancellation message to prevent delivery processing
                    sendDeliveryCancellationMessage(order.getId(), user != null ? user.getEmail() : "", "Order processing failed with exception");

                    // Update order status
                    updateOrderStatusImmediate(order.getId(), "FAILED");
                    log.error("Order Status: -> FAILED (Reason: Exception occurred)");

                    // Send failure notification with user-friendly message
                    String refundMsg = paymentProcessed ? "Payment has been refunded to your account." : "No payment was processed.";
                    sendOrderFailureNotification(order.getId(), user.getEmail(), "Order processing failed", refundMsg);

                } catch (Exception rollbackEx) {
                    log.error("Error during rollback: {}", rollbackEx.getMessage(), rollbackEx);
                }
            }

            log.error("════════════════════════════════════════════════════════════");
            // Return user-friendly error message without technical details
            return Result.error(500, "An unexpected error occurred while processing your order. Please try again later.");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelOrder(Long orderId, Long userId) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("Starting order cancellation for order: {}, user: {}", orderId, userId);
        
        try {
            // Get order
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error(404, "Order not found");
            }
            
            // Verify order belongs to user
            if (order.getUserId() != userId) {
                return Result.error(403, "Unauthorized to cancel this order");
            }
            
            // Check if order can be cancelled
            // Once delivery request is sent (DELIVERY_REQUESTED or any delivery status), order cannot be cancelled
            if ("DELIVERY_REQUESTED".equals(order.getStatus()) || 
                "PICKED_UP".equals(order.getStatus()) || 
                "IN_TRANSIT".equals(order.getStatus()) || 
                "DELIVERED".equals(order.getStatus())) {
                return Result.error(400, "Order cannot be cancelled after delivery request has been sent");
            }
            
            if ("CANCELLED".equals(order.getStatus())) {
                return Result.error(400, "Order is already cancelled");
            }
            
            if ("FAILED".equals(order.getStatus())) {
                return Result.error(400, "Order has already failed");
            }
            
            // Get user information
            User user = userMapper.selectById(userId);
            if (user == null) {
                return Result.error(404, "User not found");
            }
            
            // Release reserved stock if reservation exists
            if (order.getReservationId() != null) {
                log.info("Releasing reserved stock...");
                releaseReservedStock(String.valueOf(order.getId()), order.getReservationId(), "Order cancelled by customer");
                log.info("Stock released successfully");
            }
            
            // Process refund if payment was made
            if ("PAYMENT_SUCCESSFUL".equals(order.getStatus()) && order.getTransactionId() != null) {
                log.info("Processing refund...");
                Result<TransactionDto> refundResult = processRefund(order, user);
                
                if (refundResult.getCode() != 200) {
                    log.error("Refund failed: {}", refundResult.getMessage());
                    return Result.error(400, "Refund failed: " + refundResult.getMessage());
                }
                log.info("Refund processed successfully");
                
                // Send refund notification
                RefundNotificationDto refundNotification = RefundNotificationDto.builder()
                        .orderId(order.getId())
                        .customerEmail(user.getEmail())
                        .amount((double) order.getTotalAmount())
                        .timestamp(LocalDateTime.now())
                        .reason("Order cancelled by customer")
                        .build();
                messagePublisher.publishRefundNotification(refundNotification);
                log.info("Refund notification sent");
            }
            
            // Send delivery cancellation message to queue
            DeliveryCancellationDto cancellation = DeliveryCancellationDto.builder()
                    .orderId(order.getId())
                    .reason("Order cancelled by customer")
                    .timestamp(LocalDateTime.now())
                    .customerEmail(user.getEmail())
                    .build();
            messagePublisher.publishDeliveryCancellation(cancellation);
            log.info("Delivery cancellation message sent to queue");

            // Update order status
            String oldStatusCancel = order.getStatus();
            order.setStatus("CANCELLED");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("Order Status: {} -> CANCELLED", oldStatusCancel);

            log.info("════════════════════════════════════════════════════════════");
            log.info("Order cancellation completed successfully!");
            log.info("Order ID: {}, Status: CANCELLED", order.getId());
            log.info("════════════════════════════════════════════════════════════");

            return Result.success("Order cancelled successfully. Refund processed.", null);
            
        } catch (Exception e) {
            log.error("Order cancellation failed: {}", e.getMessage(), e);
            return Result.error(500, "Order cancellation failed: " + e.getMessage());
        }
    }

    @Override
    public void cancelOrderDueToTimeout(Long orderId, String reason) {
        log.info("════════════════════════════════════════════════════════════");
        log.info("AUTO-CANCELLATION DUE TO TIMEOUT: Order {}", orderId);
        log.info("Reason: {}", reason);

        try {
            // Get order
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.error("Order {} not found for timeout cancellation", orderId);
                return;
            }

            // Check if already cancelled or in a terminal state
            if ("CANCELLED".equals(order.getStatus()) ||
                "FAILED".equals(order.getStatus()) ||
                "DELIVERED".equals(order.getStatus())) {
                log.info("Order {} is already in terminal state: {}", orderId, order.getStatus());
                return;
            }

            // Get user information
            User user = userMapper.selectById(order.getUserId());
            if (user == null) {
                log.error("User {} not found for order {}", order.getUserId(), orderId);
                return;
            }

            // Release reserved stock if reservation exists
            if (order.getReservationId() != null) {
                log.info("Releasing reserved stock...");
                try {
                    releaseReservedStock(String.valueOf(order.getId()), order.getReservationId(), reason);
                    log.info("Stock released successfully");
                } catch (Exception e) {
                    log.error("Failed to release stock: {}", e.getMessage(), e);
                }
            }

            // Process refund if payment was made
            boolean refundProcessed = false;
            if (("PAYMENT_SUCCESSFUL".equals(order.getStatus()) ||
                 "DELIVERY_REQUESTED".equals(order.getStatus())) &&
                order.getTransactionId() != null) {
                log.info("Processing refund...");
                try {
                    Result<TransactionDto> refundResult = processRefund(order, user);

                    if (refundResult.getCode() == 200) {
                        log.info("Refund processed successfully");
                        refundProcessed = true;

                        // Send refund notification
                        RefundNotificationDto refundNotification = RefundNotificationDto.builder()
                                .orderId(order.getId())
                                .customerEmail(user.getEmail())
                                .amount((double) order.getTotalAmount())
                                .timestamp(LocalDateTime.now())
                                .reason(reason)
                                .build();
                        messagePublisher.publishRefundNotification(refundNotification);
                        log.info("Refund notification sent");
                    } else {
                        log.error("Refund failed: {}", refundResult.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Failed to process refund: {}", e.getMessage(), e);
                }
            }

            // Send delivery cancellation message to queue
            DeliveryCancellationDto cancellation = DeliveryCancellationDto.builder()
                    .orderId(order.getId())
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .customerEmail(user.getEmail())
                    .build();
            messagePublisher.publishDeliveryCancellation(cancellation);
            log.info("Delivery cancellation message sent to queue due to timeout");

            // Update order status to FAILED (not CANCELLED, to indicate system timeout)
            String oldStatus = order.getStatus();
            order.setStatus("FAILED");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("Order Status: {} -> FAILED (timeout)", oldStatus);

            // Send failure notification
            String notificationMessage = reason + (refundProcessed ? " Refund has been processed." : "");
            sendOrderFailureNotification(order.getId(), user.getEmail(), "Service Unavailable", notificationMessage);

            log.info("════════════════════════════════════════════════════════════");
            log.info("Auto-cancellation completed for order: {}", orderId);
            log.info("════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("Failed to cancel order {} due to timeout: {}", orderId, e.getMessage(), e);
        }
    }

    @Override
    public Result getOrderById(Long orderId) {
        try {
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                return Result.error(404, "Order not found");
            }
            
            // Get order items
            QueryWrapper<OrderItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("order_id", orderId);
            List<OrderItem> orderItems = orderItemMapper.selectList(queryWrapper);
            
            // Build response
            List<OrderItemDetailDto> itemDetails = orderItems.stream()
                    .map(item -> new OrderItemDetailDto(
                            item.getProductId(),
                            item.getProductName() != null ? item.getProductName() : "Product " + item.getProductId(),
                            item.getQuantity(),
                            item.getPrice(),
                            item.getPrice() * item.getQuantity()
                    ))
                    .collect(Collectors.toList());
            
            OrderResponseDto response = new OrderResponseDto();
            response.setOrderId(order.getId());
            response.setStatus(order.getStatus());
            response.setTotalAmount(order.getTotalAmount());
            response.setCreateTime(order.getCreateTime());
            response.setItems(itemDetails);
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("Failed to get order: {}", e.getMessage(), e);
            return Result.error(500, "Failed to get order: " + e.getMessage());
        }
    }

    @Override
    public Result getOrdersByUserId(Long userId) {
        try {
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.orderByDesc("create_time");
            List<Order> orders = orderMapper.selectList(queryWrapper);
            
            List<OrderResponseDto> responses = orders.stream()
                    .map(order -> {
                        // Get order items
                        QueryWrapper<OrderItem> itemQueryWrapper = new QueryWrapper<>();
                        itemQueryWrapper.eq("order_id", order.getId());
                        List<OrderItem> orderItems = orderItemMapper.selectList(itemQueryWrapper);
                        
                        List<OrderItemDetailDto> itemDetails = orderItems.stream()
                                .map(item -> new OrderItemDetailDto(
                                        item.getProductId(),
                                        item.getProductName() != null ? item.getProductName() : "Product " + item.getProductId(),
                                        item.getQuantity(),
                                        item.getPrice(),
                                        item.getPrice() * item.getQuantity()
                                ))
                                .collect(Collectors.toList());
                        
                        OrderResponseDto response = new OrderResponseDto();
                        response.setOrderId(order.getId());
                        response.setStatus(order.getStatus());
                        response.setTotalAmount(order.getTotalAmount());
                        response.setCreateTime(order.getCreateTime());
                        response.setItems(itemDetails);
                        
                        return response;
                    })
                    .collect(Collectors.toList());
            
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("Failed to get orders for user {}: {}", userId, e.getMessage(), e);
            return Result.error(500, "Failed to get orders: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeliveryStatus(Long orderId, String deliveryStatus, String message) {
        try {
            // Get order
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                log.error("Order not found: {}", orderId);
                return;
            }
            
            String oldStatus = order.getStatus();
            
            // Update order status based on delivery status
            switch (deliveryStatus) {
                case "REQUEST_RECEIVED":
                    // Delivery request confirmed by DeliveryCo
                    log.info("Order Status: {} (DeliveryCo confirmed request)", oldStatus);
                    break;
                    
                case "PICKED_UP":
                    // Package picked up from warehouse
                    order.setStatus("PICKED_UP");
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("Order Status: {} -> PICKED_UP", oldStatus);
                    break;
                    
                case "IN_TRANSIT":
                    // Package is on the delivery truck
                    order.setStatus("IN_TRANSIT");
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("Order Status: {} -> IN_TRANSIT", oldStatus);
                    break;
                    
                case "DELIVERED":
                    // Package successfully delivered to customer
                    order.setStatus("DELIVERED");
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("Order Status: {} -> DELIVERED", oldStatus);
                    break;
                    
                case "LOST":
                    // Package lost during delivery - need to handle refund
                    log.warn("Package lost for order: {}", orderId);
                    
                    // Get user information for refund
                    User user = userMapper.selectById(order.getUserId());
                    if (user == null) {
                        log.error("User not found for order: {}", orderId);
                        break;
                    }
                    
                    // Process refund if payment was made
                    if (order.getTransactionId() != null) {
                        log.info("Processing refund for lost package...");
                        Result<TransactionDto> refundResult = processRefund(order, user);
                        
                        if (refundResult.getCode() == 200) {
                            log.info("Refund processed successfully");
                            // Send refund notification
                            RefundNotificationDto refundNotification = RefundNotificationDto.builder()
                                    .orderId(order.getId())
                                    .customerEmail(user.getEmail())
                                    .amount((double) order.getTotalAmount())
                                    .timestamp(LocalDateTime.now())
                                    .reason("Package lost during delivery")
                                    .build();
                            messagePublisher.publishRefundNotification(refundNotification);
                            log.info("Refund notification sent");
                        } else {
                            log.error("Refund failed: {}", refundResult.getMessage());
                            // Send notification to customer service for manual handling
                            sendOrderFailureNotification(order.getId(), user.getEmail(), 
                                "Package lost - refund failed", 
                                "Please contact customer service. Refund error: " + refundResult.getMessage());
                        }
                    }
                    
                    // Update order status to LOST
                    order.setStatus("LOST");
                    order.setUpdateTime(LocalDateTime.now());
                    orderMapper.updateById(order);
                    log.info("Order Status: {} -> LOST", oldStatus);
                    break;
                    
                default:
                    log.warn("Unknown delivery status: {}", deliveryStatus);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Failed to update delivery status for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

    // ============= Private Helper Methods =============

    private OrderValidationRequest buildValidationRequest(OrderCreateRequestDTO request) {
        List<OrderValidationRequest.OrderItem> items = request.getItems().stream()
                .map(item -> new OrderValidationRequest.OrderItem(item.getProductId(), item.getQuantity()))
                .collect(Collectors.toList());
        
        return new OrderValidationRequest(UUID.randomUUID().toString(), items);
    }

    /**
     * Create order in a new transaction - immediately visible to queries
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected OrderResponseDto createOrderTransaction(OrderCreateRequestDTO request, User user) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("PENDING_VALIDATION");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        double totalAmount = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemDetailDto> itemDetails = new ArrayList<>();

        for (OrderItemRequestDTO itemReq : request.getItems()) {
            // Get product price from warehouse
            Result<ProductPriceResponse> productResult = warehouseClient.getProductPrice(itemReq.getProductId(), null);
            
            if (productResult.getCode() != 200 || productResult.getData() == null) {
                throw new RuntimeException("Failed to get product price for product: " + itemReq.getProductId());
            }
            
            ProductPriceResponse product = productResult.getData();
            double price = product.getPrice().doubleValue();
            double subTotalAmount = price * itemReq.getQuantity();
            totalAmount += subTotalAmount;

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(price);
            orderItems.add(orderItem);

            OrderItemDetailDto detailDTO = new OrderItemDetailDto(
                    product.getId(),
                    product.getName(),
                    itemReq.getQuantity(),
                    price,
                    subTotalAmount
            );
            itemDetails.add(detailDTO);
        }

        order.setTotalAmount(totalAmount);
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setTotalAmount(order.getTotalAmount());
        responseDto.setCreateTime(order.getCreateTime());
        responseDto.setItems(itemDetails);

        return responseDto;
    }

    private Result<TransactionDto> processPayment(Order order, User user) {
        BankRequestDto bankRequest = new BankRequestDto();
        bankRequest.setOrderId(String.valueOf(order.getId()));
        bankRequest.setUserId(String.valueOf(user.getId()));
        bankRequest.setAmount(BigDecimal.valueOf(order.getTotalAmount()));
        bankRequest.setCurrency("AUD");

        return bankClient.handleDebit(bankRequest);
    }

    private Result<TransactionDto> processRefund(Order order, User user) {
        BankRequestDto refundRequest = new BankRequestDto();
        refundRequest.setOrderId(String.valueOf(order.getId()));
        refundRequest.setUserId(String.valueOf(user.getId()));
        refundRequest.setAmount(BigDecimal.valueOf(order.getTotalAmount()));
        refundRequest.setCurrency("AUD");
        // Set idempotency key to prevent duplicate refunds for the same order
        refundRequest.setIdempotencyKey("REFUND-" + order.getId());

        return bankClient.handleRefund(refundRequest);
    }

    private void confirmReservation(String orderId, String reservationId) {
        ConfirmReservationRequest confirmRequest = new ConfirmReservationRequest(orderId, reservationId);
        Result<ConfirmReservationResponse> confirmResult = warehouseClient.confirmReservation(confirmRequest);
        
        if (confirmResult.getCode() != 200) {
            log.warn("Failed to confirm reservation: {}", confirmResult.getMessage());
        }
    }

    private void releaseReservedStock(String orderId, String reservationId, String reason) {
        try {
            ReleaseStockRequest releaseRequest = new ReleaseStockRequest(orderId, reservationId, reason);
            Result<ReleaseStockResponse> releaseResult = warehouseClient.releaseStock(releaseRequest);
            
            if (releaseResult.getCode() != 200) {
                log.error("Failed to release stock: {}", releaseResult.getMessage());
            }
        } catch (Exception e) {
            log.error("Error releasing stock: {}", e.getMessage(), e);
        }
    }

    private DeliveryRequestDto buildDeliveryRequest(Order order, OrderCreateRequestDTO request, User user) {
        Set<Long> warehouseIds = new HashSet<>();
        List<DeliveryRequestDto.ProductInfo> products = new ArrayList<>();
        for (OrderItemRequestDTO item : request.getItems()) {
            long warehouseId = 1L;
            warehouseIds.add(warehouseId);
            String productName = "Product " + item.getProductId();
            try {
                Result<ProductPriceResponse> productResult = warehouseClient.getProductPrice(item.getProductId(), null);
                if (productResult.getCode() == 200 && productResult.getData() != null) {
                    productName = productResult.getData().getName();
                }
            } catch (Exception e) {
                log.warn("Failed to get product name for product: {}", item.getProductId());
            }
            DeliveryRequestDto.ProductInfo productInfo = DeliveryRequestDto.ProductInfo.builder()
                .productId(item.getProductId())
                .productName(productName)
                .quantity(item.getQuantity())
                .warehouseId(warehouseId)
                .build();
            products.add(productInfo);
        }
        return DeliveryRequestDto.builder()
            .orderId(order.getId())
            .customerId(user.getId())
            .customerEmail(user.getEmail())
            .warehouseIds(new ArrayList<>(warehouseIds))
            .products(products)
            .build();
    }

    private void sendOrderFailureNotification(Long orderId, String customerEmail, String reason, String errorDetails) {
        try {
            OrderFailureNotificationDto notification = OrderFailureNotificationDto.builder()
                    .orderId(orderId)
                    .customerEmail(customerEmail)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .errorDetails(errorDetails)
                    .build();
            messagePublisher.publishOrderFailureNotification(notification);
        } catch (Exception e) {
            log.error("Failed to send order failure notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Process payment and update order status in a new transaction - immediately visible
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected Result<TransactionDto> processPaymentAndUpdateStatus(Long orderId, User user) {
        Order order = orderMapper.selectById(orderId);
        
        // Double-check the order hasn't been cancelled
        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("Order {} is already cancelled. Skipping payment processing.", orderId);
            return Result.error(400, "Order has been cancelled");
        }
        
        Result<TransactionDto> paymentResult = processPayment(order, user);
        
        if (paymentResult.getCode() == 200) {
            order.setTransactionId(String.valueOf(paymentResult.getData().getId()));
            order.setStatus("PAYMENT_SUCCESSFUL");
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
        
        return paymentResult;
    }

    /**
     * Send delivery request and update order status in a new transaction - immediately visible
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void sendDeliveryRequestAndUpdateStatus(Long orderId, OrderCreateRequestDTO request, User user) {
        Order order = orderMapper.selectById(orderId);
        if ("CANCELLED".equals(order.getStatus())) {
            log.warn("Order {} is already cancelled. Skipping delivery request.", orderId);
            throw new BusinessException(400, "Order has been cancelled");
        }
        DeliveryRequestDto deliveryRequest = buildDeliveryRequest(order, request, user);
        messagePublisher.publishDeliveryRequest(deliveryRequest);
        order.setStatus("DELIVERY_REQUESTED");
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * Update order status in a new transaction - immediately visible
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected void updateOrderStatusImmediate(Long orderId, String newStatus) {
        Order order = orderMapper.selectById(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    /**
     * Send delivery cancellation message to prevent processing of cancelled orders
     * This method safely sends cancellation messages and logs errors without throwing exceptions
     *
     * @param orderId Order ID to cancel
     * @param customerEmail Customer email for notifications
     * @param reason Cancellation reason
     */
    private void sendDeliveryCancellationMessage(Long orderId, String customerEmail, String reason) {
        try {
            DeliveryCancellationDto cancellation = DeliveryCancellationDto.builder()
                    .orderId(orderId)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .customerEmail(customerEmail != null ? customerEmail : "")
                    .build();
            messagePublisher.publishDeliveryCancellation(cancellation);
            log.info("Delivery cancellation message sent for order {}: {}", orderId, reason);
        } catch (Exception e) {
            // Don't throw exception - just log the error
            // We don't want to fail the order cancellation process just because we couldn't send the message
            log.error("Failed to send delivery cancellation message for order {}: {}", orderId, e.getMessage());
        }
    }
}
