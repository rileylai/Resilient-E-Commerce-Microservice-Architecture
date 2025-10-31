package com.tut2.group3.store.service;

import com.tut2.group3.store.config.RabbitMQConfig;
import com.tut2.group3.store.dto.deliveryco.DeliveryCancellationDto;
import com.tut2.group3.store.dto.deliveryco.DeliveryRequestDto;
import com.tut2.group3.store.dto.email.OrderFailureNotificationDto;
import com.tut2.group3.store.dto.email.RefundNotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Message Publisher Service
 * 
 * Publishes messages to RabbitMQ for delivery requests,
 * order failures, and refund notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish delivery request to DeliveryCo service
     */
    public void publishDeliveryRequest(DeliveryRequestDto deliveryRequest) {
        try {
            log.info("Publishing delivery request for order: {}", deliveryRequest.getOrderId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELIVERY_EXCHANGE,
                    RabbitMQConfig.DELIVERY_REQUEST_ROUTING_KEY,
                    deliveryRequest
            );
            log.info("Delivery request published successfully");
        } catch (Exception e) {
            log.error("Failed to publish delivery request for order: {}", deliveryRequest.getOrderId(), e);
            throw new RuntimeException("Failed to publish delivery request", e);
        }
    }

    /**
     * Publish delivery cancellation to DeliveryCo service
     */
    public void publishDeliveryCancellation(DeliveryCancellationDto cancellation) {
        try {
            log.info("Publishing delivery cancellation for order: {}", cancellation.getOrderId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELIVERY_EXCHANGE,
                    RabbitMQConfig.DELIVERY_CANCELLATION_ROUTING_KEY,
                    cancellation
            );
            log.info("Delivery cancellation published successfully");
        } catch (Exception e) {
            log.error("Failed to publish delivery cancellation for order: {}", cancellation.getOrderId(), e);
            // Don't throw exception - cancellation message failure shouldn't block the cancellation process
        }
    }

    /**
     * Publish order failure notification to EmailService
     */
    public void publishOrderFailureNotification(OrderFailureNotificationDto notification) {
        try {
            log.info("Publishing order failure notification for order: {}", notification.getOrderId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.STORE_EXCHANGE,
                    RabbitMQConfig.ORDER_FAILURE_ROUTING_KEY,
                    notification
            );
            log.info("Order failure notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish order failure notification for order: {}", notification.getOrderId(), e);
            // Don't throw exception - email failure shouldn't block the process
        }
    }

    /**
     * Publish refund notification to EmailService
     */
    public void publishRefundNotification(RefundNotificationDto notification) {
        try {
            log.info("Publishing refund notification for order: {}", notification.getOrderId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.STORE_EXCHANGE,
                    RabbitMQConfig.REFUND_ROUTING_KEY,
                    notification
            );
            log.info("Refund notification published successfully");
        } catch (Exception e) {
            log.error("Failed to publish refund notification for order: {}", notification.getOrderId(), e);
            // Don't throw exception - email failure shouldn't block the process
        }
    }
}

