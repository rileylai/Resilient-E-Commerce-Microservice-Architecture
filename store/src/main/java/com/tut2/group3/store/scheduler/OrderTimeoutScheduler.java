package com.tut2.group3.store.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tut2.group3.store.mapper.OrderMapper;
import com.tut2.group3.store.mapper.UserMapper;
import com.tut2.group3.store.pojo.Order;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.pojo.User;
import com.tut2.group3.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to monitor and handle order timeouts
 * Checks for orders that have been stuck in processing states for more than 15 seconds
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final OrderService orderService;

    // Timeout threshold in seconds
    private static final int TIMEOUT_SECONDS = 15;

    /**
     * Runs every 5 seconds to check for timed out orders
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void checkTimeoutOrders() {
        try {
            // Find orders that are stuck in processing states
            QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("status",
                "PENDING_VALIDATION",
                "PENDING_PAYMENT",
                "PAYMENT_SUCCESSFUL",
                "DELIVERY_REQUESTED"
            );

            List<Order> processingOrders = orderMapper.selectList(queryWrapper);

            LocalDateTime now = LocalDateTime.now();

            for (Order order : processingOrders) {
                LocalDateTime updateTime = order.getUpdateTime();
                if (updateTime == null) {
                    updateTime = order.getCreateTime();
                }

                // Calculate time difference in seconds
                long secondsPassed = java.time.Duration.between(updateTime, now).getSeconds();

                if (secondsPassed > TIMEOUT_SECONDS) {
                    log.warn("Order {} has timed out in status {} after {} seconds. Initiating auto-rollback.",
                        order.getId(), order.getStatus(), secondsPassed);

                    handleTimeoutOrder(order);
                }
            }
        } catch (Exception e) {
            log.error("Error in timeout scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle an order that has timed out
     * Performs rollback operations based on current order status
     */
    private void handleTimeoutOrder(Order order) {
        try {
            String currentStatus = order.getStatus();
            User user = userMapper.selectById(order.getUserId());

            if (user == null) {
                log.error("Cannot handle timeout for order {}: User not found", order.getId());
                return;
            }

            log.info("════════════════════════════════════════════════════════════");
            log.info("AUTO-ROLLBACK: Order {} timed out in status {}", order.getId(), currentStatus);
            log.info("════════════════════════════════════════════════════════════");

            // Perform rollback based on current status
            switch (currentStatus) {
                case "PENDING_VALIDATION":
                case "PENDING_PAYMENT":
                    // Stock was reserved but payment not processed
                    handleTimeoutBeforePayment(order, user);
                    break;

                case "PAYMENT_SUCCESSFUL":
                case "DELIVERY_REQUESTED":
                    // Payment was processed, need to refund
                    handleTimeoutAfterPayment(order, user);
                    break;

                default:
                    log.warn("Order {} in status {} - no timeout handling defined",
                        order.getId(), currentStatus);
            }
        } catch (Exception e) {
            log.error("Failed to handle timeout for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Handle timeout before payment was processed
     */
    private void handleTimeoutBeforePayment(Order order, User user) {
        try {
            // Directly perform cancellation with timeout reason
            orderService.cancelOrderDueToTimeout(order.getId(), "Service timeout - No response received within 15 seconds");
            log.info("Successfully auto-cancelled order {} due to timeout (before payment)", order.getId());
        } catch (Exception e) {
            log.error("Error during auto-cancel for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }

    /**
     * Handle timeout after payment was processed
     * Need to refund and release stock
     */
    private void handleTimeoutAfterPayment(Order order, User user) {
        try {
            // Directly perform cancellation with timeout reason (includes refund)
            orderService.cancelOrderDueToTimeout(order.getId(), "Service timeout - No response received within 15 seconds");
            log.info("Successfully auto-cancelled and refunded order {} due to timeout (after payment)", order.getId());
        } catch (Exception e) {
            log.error("Error during auto-cancel and refund for order {}: {}", order.getId(), e.getMessage(), e);
        }
    }
}
