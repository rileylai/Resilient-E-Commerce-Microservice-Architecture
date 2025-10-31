package com.tut2.group3.deliveryco.scheduler;

import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import com.tut2.group3.deliveryco.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Delivery Scheduler
 *
 * Automatically progresses delivery status every 5 seconds.
 * Handles REQUEST_RECEIVED → PICKED_UP → IN_TRANSIT → DELIVERED/LOST.
 */
@Slf4j
@Component
public class DeliveryScheduler {

    @Autowired
    private DeliveryService deliveryService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Process delivery status progression
     * Runs every 5 seconds (5000 milliseconds)
     *
     * Status progression:
     * REQUEST_RECEIVED → PICKED_UP → IN_TRANSIT → DELIVERED (95%) / LOST (5%)
     */
    @Scheduled(fixedDelay = 5000)
    public void processDeliveryStatusProgression() {
        try {
            // Find all deliveries that need status progression
            List<Delivery> pendingDeliveries = new ArrayList<>();

            // Add deliveries in REQUEST_RECEIVED status
            List<Delivery> requestReceived = deliveryService.getDeliveriesByStatus(DeliveryStatus.REQUEST_RECEIVED);
            if (requestReceived != null && !requestReceived.isEmpty()) {
                pendingDeliveries.addAll(requestReceived);
            }

            // Add deliveries in PICKED_UP status
            List<Delivery> pickedUp = deliveryService.getDeliveriesByStatus(DeliveryStatus.PICKED_UP);
            if (pickedUp != null && !pickedUp.isEmpty()) {
                pendingDeliveries.addAll(pickedUp);
            }

            // Add deliveries in IN_TRANSIT status
            List<Delivery> inTransit = deliveryService.getDeliveriesByStatus(DeliveryStatus.IN_TRANSIT);
            if (inTransit != null && !inTransit.isEmpty()) {
                pendingDeliveries.addAll(inTransit);
            }

            // If no pending deliveries, skip silently
            if (pendingDeliveries.isEmpty()) {
                return; // No log output when nothing to process
            }

            log.info("════════════════════════════════════════════════════════════════");
            log.info("[{}] Scheduler running - Processing {} pending deliveries",
                    getCurrentTimestamp(),
                    pendingDeliveries.size());

            // Process each pending delivery concurrently
            for (Delivery delivery : pendingDeliveries) {
                processDeliveryAsync(delivery);
            }

            log.info("[{}] Scheduler dispatched {} deliveries for concurrent processing",
                    getCurrentTimestamp(),
                    pendingDeliveries.size());
            log.info("════════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("[{}] Scheduler error: {}", getCurrentTimestamp(), e.getMessage(), e);
        }
    }

    /**
     * Process single delivery asynchronously
     * Allows multiple deliveries to be processed concurrently
     */
    @Async
    public void processDeliveryAsync(Delivery delivery) {
        try {
            DeliveryStatus oldStatus = delivery.getStatus();

            // Process status progression
            deliveryService.processDeliveryStatusProgression(delivery);

            // Reload delivery to get updated status
            Delivery updatedDelivery = deliveryService.getDeliveryById(delivery.getId());
            DeliveryStatus newStatus = updatedDelivery.getStatus();

            log.info("[{}] Success DeliveryID: {} | OrderID: {} | {} → {}",
                    getCurrentTimestamp(),
                    delivery.getId(),
                    delivery.getOrderId(),
                    oldStatus,
                    newStatus);

        } catch (Exception e) {
            log.error("[{}] Error processing DeliveryID: {} - {}",
                    getCurrentTimestamp(),
                    delivery.getId(),
                    e.getMessage());
        }
    }

    /**
     * Get current timestamp as formatted string
     */
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
