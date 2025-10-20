package com.tut2.group3.deliveryco.mq;

import com.tut2.group3.deliveryco.config.RabbitMQConfig;
import com.tut2.group3.deliveryco.dto.DeliveryRequestDTO;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Delivery Message Listener
 *
 * Listens to delivery.request.queue from Store service.
 * Creates delivery records and sends initial notifications.
 */
@Slf4j
@Component
public class DeliveryMessageListener {

    @Autowired
    private DeliveryService deliveryService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Handle delivery request from Store service
     * Triggered when Store successfully processes payment and sends delivery request
     *
     * @param request Delivery request DTO containing order and customer information
     */
    @RabbitListener(queues = RabbitMQConfig.DELIVERY_REQUEST_QUEUE)
    public void handleDeliveryRequest(DeliveryRequestDTO request) {
        log.info("════════════════════════════════════════════════════════════════");
        log.info("[{}] Received delivery request from Store", getCurrentTimestamp());
        log.info("[{}] OrderID: {}, CustomerID: {}, Email: {}",
                getCurrentTimestamp(),
                request.getOrderId(),
                request.getCustomerId(),
                request.getCustomerEmail());
        log.info("[{}] Warehouses: {}, Products: {}",
                getCurrentTimestamp(),
                request.getWarehouseIds(),
                request.getProducts().size());
        log.info("════════════════════════════════════════════════════════════════");

        try {
            // Create delivery record and send initial notification
            Delivery delivery = deliveryService.createDelivery(request);

            log.info("[{}] Delivery created successfully - DeliveryID: {}, Status: {}",
                    getCurrentTimestamp(),
                    delivery.getId(),
                    delivery.getStatus());
            log.info("[{}] Notification sent to Store and EmailService",
                    getCurrentTimestamp());

        } catch (Exception e) {
            log.error("════════════════════════════════════════════════════════════════");
            log.error("[{}] Failed to process delivery request for OrderID: {}",
                    getCurrentTimestamp(),
                    request.getOrderId());
            log.error("[{}] Error: {}", getCurrentTimestamp(), e.getMessage(), e);
            log.error("════════════════════════════════════════════════════════════════");
        }
    }

    /**
     * Get current timestamp as formatted string
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
