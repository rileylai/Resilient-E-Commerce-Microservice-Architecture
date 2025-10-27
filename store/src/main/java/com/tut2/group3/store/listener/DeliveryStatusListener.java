package com.tut2.group3.store.listener;

import com.tut2.group3.store.config.RabbitMQConfig;
import com.tut2.group3.store.dto.deliveryco.DeliveryStatusUpdateDTO;
import com.tut2.group3.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for delivery status updates from DeliveryCo
 * Listens to the delivery.status.queue and updates order status accordingly
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryStatusListener {

    private final OrderService orderService;

    /**
     * Handles delivery status updates from DeliveryCo
     * 
     * @param statusUpdate Delivery status update DTO containing order ID, new status, and message
     */
    @RabbitListener(queues = RabbitMQConfig.DELIVERY_STATUS_QUEUE)
    public void handleDeliveryStatusUpdate(DeliveryStatusUpdateDTO statusUpdate) {
        log.info("----------------------------------------------------------------");
        log.info("📦 Delivery Status Update Received");
        log.info("Order ID: {}", statusUpdate.getOrderId());
        log.info("New Status: {}", statusUpdate.getNewStatus());
        log.info("Customer: {}", statusUpdate.getCustomerEmail());
        log.info("Message: {}", statusUpdate.getMessage() != null ? statusUpdate.getMessage() : "N/A");
        log.info("Timestamp: {}", statusUpdate.getTimestamp());
        log.info("----------------------------------------------------------------");

        try {
            // Validate the status update
            if (statusUpdate.getOrderId() == null) {
                log.error("Invalid status update: Order ID is null");
                return;
            }
            
            if (statusUpdate.getNewStatus() == null || statusUpdate.getNewStatus().trim().isEmpty()) {
                log.error("Invalid status update: Status is null or empty");
                return;
            }

            // Call OrderService to update the order status
            orderService.updateDeliveryStatus(
                    statusUpdate.getOrderId(),
                    statusUpdate.getNewStatus(),
                    statusUpdate.getMessage()
            );

            log.info("✓ Successfully processed delivery status update for order: {}", 
                    statusUpdate.getOrderId());

        } catch (Exception e) {
            log.error("❌ Failed to process delivery status update for order {}: {}", 
                    statusUpdate.getOrderId(), e.getMessage(), e);
            // Note: In production, you might want to implement retry logic or dead letter queue
        }
    }
}

