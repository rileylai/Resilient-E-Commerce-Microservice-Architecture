package com.tut2.group3.deliveryco.listener;

import com.tut2.group3.deliveryco.config.RabbitMQConfig;
import com.tut2.group3.deliveryco.dto.DeliveryCancellationDto;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import com.tut2.group3.deliveryco.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for delivery cancellation messages from Store service
 * Handles cancellation of delivery orders
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryCancellationListener {

    private final DeliveryRepository deliveryRepository;

    /**
     * Handles delivery cancellation from Store service
     * Prevents processing cancelled orders when DeliveryCo restarts
     *
     * @param cancellation Delivery cancellation DTO containing order ID and reason
     */
    @RabbitListener(queues = RabbitMQConfig.DELIVERY_CANCELLATION_QUEUE)
    public void handleDeliveryCancellation(DeliveryCancellationDto cancellation) {
        log.info("================================================================");
        log.info("Delivery Cancellation Received");
        log.info("Order ID: {}", cancellation.getOrderId());
        log.info("Reason: {}", cancellation.getReason());
        log.info("Timestamp: {}", cancellation.getTimestamp());
        log.info("================================================================");

        try {
            // Validate the cancellation
            if (cancellation.getOrderId() == null) {
                log.error("Invalid cancellation: Order ID is null");
                return;
            }

            // Check if delivery exists for this order
            Delivery delivery = deliveryRepository.findByOrderId(cancellation.getOrderId());

            if (delivery == null) {
                log.info("No delivery found for order {}. Creating CANCELLED placeholder to prevent future delivery processing.",
                        cancellation.getOrderId());

                // Create a CANCELLED delivery record to prevent the delivery request from being processed
                Delivery cancelledDelivery = new Delivery();
                cancelledDelivery.setOrderId(cancellation.getOrderId());
                cancelledDelivery.setCustomerId(0L); // Placeholder - we don't have customer info from cancellation
                cancelledDelivery.setCustomerEmail(cancellation.getCustomerEmail());
                cancelledDelivery.setStatus(DeliveryStatus.CANCELLED);

                try {
                    deliveryRepository.insert(cancelledDelivery);
                    log.info("Created CANCELLED delivery record for order {} to block future delivery requests",
                            cancellation.getOrderId());
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // Another thread (delivery request) inserted a record at the same time
                    // Re-query and try to update it to CANCELLED
                    log.warn("Concurrent insert detected for order {}. Re-querying to update status.",
                            cancellation.getOrderId());

                    delivery = deliveryRepository.findByOrderId(cancellation.getOrderId());
                    if (delivery != null && delivery.getStatus() != DeliveryStatus.CANCELLED) {
                        log.info("Updating existing delivery {} to CANCELLED status", delivery.getId());
                        delivery.setStatus(DeliveryStatus.CANCELLED);
                        deliveryRepository.updateById(delivery);
                    }
                }
                return;
            }

            // Check current delivery status
            DeliveryStatus currentStatus = delivery.getStatus();
            log.info("Current delivery status for order {}: {}", cancellation.getOrderId(), currentStatus);

            // Only cancel if not already in a terminal state
            if (currentStatus == DeliveryStatus.DELIVERED ||
                currentStatus == DeliveryStatus.LOST ||
                currentStatus == DeliveryStatus.CANCELLED) {
                log.warn("Delivery for order {} is already in terminal state: {}. Cannot cancel.",
                        cancellation.getOrderId(), currentStatus);
                return;
            }

            // Update delivery status to CANCELLED
            delivery.setStatus(DeliveryStatus.CANCELLED);
            deliveryRepository.updateById(delivery);

            log.info("Successfully cancelled delivery for order: {}", cancellation.getOrderId());
            log.info("Previous status: {} -> New status: CANCELLED", currentStatus);

        } catch (Exception e) {
            log.error("Failed to process delivery cancellation for order {}: {}",
                    cancellation.getOrderId(), e.getMessage(), e);
            // Note: In production, you might want to implement retry logic or dead letter queue
        }
    }
}
