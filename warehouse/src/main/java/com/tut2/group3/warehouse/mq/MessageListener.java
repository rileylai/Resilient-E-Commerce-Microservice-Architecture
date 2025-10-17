package com.tut2.group3.warehouse.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tut2.group3.warehouse.dto.request.ReleaseStockRequest;
import com.tut2.group3.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageListener {

    private final WarehouseService warehouseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen for delivery pickup confirmation events
     * When delivery is picked up, log it as the stock is already confirmed
     */
    @RabbitListener(queues = "warehouse.delivery.pickup")
    public void handleDeliveryPickupConfirmed(String message) {
        try {
            log.info("Received delivery pickup confirmation: {}", message);

            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.get("orderId").asText();
            Long warehouseId = jsonNode.get("warehouseId").asLong();
            String trackingNumber = jsonNode.has("trackingNumber")
                    ? jsonNode.get("trackingNumber").asText()
                    : "N/A";

            log.info("Delivery picked up for order {} from warehouse {}. Tracking: {}",
                    orderId, warehouseId, trackingNumber);

            // The stock was already deducted during confirmation
            // This event is just for logging/tracking purposes

        } catch (Exception e) {
            log.error("Error processing delivery pickup confirmation", e);
            throw new RuntimeException("Failed to process delivery pickup confirmation", e);
        }
    }

    /**
     * Listen for order cancelled events
     * Automatically release reserved stock for cancelled orders
     */
    @RabbitListener(queues = "warehouse.order.cancelled")
    public void handleOrderCancelled(String message) {
        try {
            log.info("Received order cancelled event: {}", message);

            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.get("orderId").asText();
            String reason = jsonNode.has("reason")
                    ? jsonNode.get("reason").asText()
                    : "ORDER_CANCELLED";

            // Generate a reservation ID for the cancellation
            String reservationId = "AUTO-RELEASE-" + System.currentTimeMillis();

            // Create release request
            ReleaseStockRequest releaseRequest = new ReleaseStockRequest(
                    orderId,
                    reservationId,
                    reason
            );

            // Release the stock
            warehouseService.releaseStock(releaseRequest);

            log.info("Successfully released stock for cancelled order {}", orderId);

        } catch (Exception e) {
            log.error("Error processing order cancelled event", e);
            // Don't throw exception to avoid infinite retry loop
            // The message will be sent to DLQ after max retries
        }
    }
}
