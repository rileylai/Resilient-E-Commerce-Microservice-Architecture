package com.tut2.group3.deliveryco.service.impl;

import com.tut2.group3.deliveryco.config.RabbitMQConfig;
import com.tut2.group3.deliveryco.dto.DeliveryRequestDTO;
import com.tut2.group3.deliveryco.dto.DeliveryStatusUpdateDTO;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import com.tut2.group3.deliveryco.repository.DeliveryRepository;
import com.tut2.group3.deliveryco.service.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Delivery Service Implementation
 *
 * Handles delivery creation, status updates, and notifications.
 * Simulates package loss based on configurable loss rate.
 */
@Slf4j
@Service
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Package loss rate percentage (0-100)
     * Configurable via application.properties: delivery.package-loss-rate
     * Default: 5% as per assignment requirements
     */
    @Value("${delivery.package-loss-rate:5}")
    private int packageLossRate;

    private static final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public Delivery createDelivery(DeliveryRequestDTO request) {
        log.info("[{}] Creating delivery for OrderID: {}, CustomerID: {}, Warehouses: {}",
                getCurrentTimestamp(),
                request.getOrderId(),
                request.getCustomerId(),
                request.getWarehouseIds());

        // Create new delivery entity
        Delivery delivery = new Delivery();
        delivery.setOrderId(request.getOrderId());
        delivery.setCustomerId(request.getCustomerId());
        delivery.setCustomerEmail(request.getCustomerEmail());

        // Convert warehouse IDs list to comma-separated string
        String warehouseIds = request.getWarehouseIds().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        delivery.setWarehouseIds(warehouseIds);

        delivery.setStatus(DeliveryStatus.REQUEST_RECEIVED);
        // createdAt and updatedAt are auto-filled by MyBatis-Plus MetaObjectHandler

        // Save to database
        deliveryRepository.insert(delivery);

        log.info("[{}] Delivery created successfully - DeliveryID: {}, OrderID: {}, Status: REQUEST_RECEIVED",
                getCurrentTimestamp(),
                delivery.getId(),
                delivery.getOrderId());

        // Send initial status notification
        sendStatusUpdateNotification(delivery, request.getCustomerEmail(),
                "Delivery request received. Your order is being processed.");

        return delivery;
    }

    @Override
    @Transactional
    public Delivery updateDeliveryStatus(Long deliveryId, DeliveryStatus newStatus) {
        Delivery delivery = deliveryRepository.selectById(deliveryId);
        if (delivery == null) {
            log.error("[{}] Delivery not found - DeliveryID: {}", getCurrentTimestamp(), deliveryId);
            throw new RuntimeException("Delivery not found with ID: " + deliveryId);
        }

        DeliveryStatus oldStatus = delivery.getStatus();

        // Prevent invalid status transitions
        if (oldStatus == DeliveryStatus.DELIVERED || oldStatus == DeliveryStatus.LOST) {
            log.warn("[{}] Cannot update delivery in final status - DeliveryID: {}, CurrentStatus: {}",
                    getCurrentTimestamp(), deliveryId, oldStatus);
            return delivery;
        }

        log.info("[{}] Updating delivery status - DeliveryID: {}, OrderID: {}, {} → {}",
                getCurrentTimestamp(),
                deliveryId,
                delivery.getOrderId(),
                oldStatus,
                newStatus);

        // Update status
        delivery.setStatus(newStatus);
        // updatedAt is auto-filled by MyBatis-Plus MetaObjectHandler
        deliveryRepository.updateById(delivery);

        log.info("[{}] Delivery status updated successfully - DeliveryID: {}, OrderID: {}, NewStatus: {}",
                getCurrentTimestamp(),
                delivery.getId(),
                delivery.getOrderId(),
                newStatus);

        // Send notification with customer email from delivery record
        String message = generateStatusMessage(newStatus);
        sendStatusUpdateNotification(delivery, delivery.getCustomerEmail(), message);

        return delivery;
    }

    @Override
    public Delivery getDeliveryByOrderId(Long orderId) {
        log.info("[{}] Querying delivery by OrderID: {}", getCurrentTimestamp(), orderId);
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public List<Delivery> getDeliveriesByCustomerId(Long customerId) {
        log.info("[{}] Querying deliveries for CustomerID: {}", getCurrentTimestamp(), customerId);
        return deliveryRepository.findByCustomerId(customerId);
    }

    @Override
    public List<Delivery> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findByStatus(status);
    }

    @Override
    public Delivery getDeliveryById(Long deliveryId) {
        log.info("[{}] Querying delivery by DeliveryID: {}", getCurrentTimestamp(), deliveryId);
        return deliveryRepository.selectById(deliveryId);
    }

    @Override
    public List<Delivery> getAllDeliveries() {
        log.info("[{}] Querying all deliveries", getCurrentTimestamp());
        return deliveryRepository.selectList(null);
    }

    @Override
    @Transactional
    public void processDeliveryStatusProgression(Delivery delivery) {
        DeliveryStatus currentStatus = delivery.getStatus();

        // Skip if already in final state
        if (currentStatus == DeliveryStatus.DELIVERED || currentStatus == DeliveryStatus.LOST) {
            return;
        }

        DeliveryStatus nextStatus = determineNextStatus(currentStatus);

        log.info("[{}] Processing automatic status progression - DeliveryID: {}, OrderID: {}, {} → {}",
                getCurrentTimestamp(),
                delivery.getId(),
                delivery.getOrderId(),
                currentStatus,
                nextStatus);

        updateDeliveryStatus(delivery.getId(), nextStatus);
    }

    @Override
    public DeliveryStatus determineNextStatus(DeliveryStatus currentStatus) {
        switch (currentStatus) {
            case REQUEST_RECEIVED:
                return DeliveryStatus.PICKED_UP;

            case PICKED_UP:
                return DeliveryStatus.IN_TRANSIT;

            case IN_TRANSIT:
                // Simulate package loss based on configured rate
                int randomValue = random.nextInt(100);
                if (randomValue < packageLossRate) {
                    log.warn("[{}] Package loss simulated ({}% chance)", getCurrentTimestamp(), packageLossRate);
                    return DeliveryStatus.LOST;
                } else {
                    return DeliveryStatus.DELIVERED;
                }

            default:
                // Already in final state or unknown state
                return currentStatus;
        }
    }

    private void sendStatusUpdateNotification(Delivery delivery, String customerEmail, String message) {
        DeliveryStatusUpdateDTO statusUpdate = new DeliveryStatusUpdateDTO();
        statusUpdate.setOrderId(delivery.getOrderId());
        statusUpdate.setNewStatus(delivery.getStatus().name());
        statusUpdate.setCustomerEmail(customerEmail);
        statusUpdate.setTimestamp(LocalDateTime.now());
        statusUpdate.setMessage(message);

        try {
            // Send to Store service
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELIVERY_EXCHANGE,
                    RabbitMQConfig.DELIVERY_STATUS_ROUTING_KEY,
                    statusUpdate
            );
            log.info("[{}] Status update sent to Store - OrderID: {}, Status: {}",
                    getCurrentTimestamp(),
                    delivery.getOrderId(),
                    delivery.getStatus());

            // Send to EmailService
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DELIVERY_EXCHANGE,
                    RabbitMQConfig.DELIVERY_EMAIL_ROUTING_KEY,
                    statusUpdate
            );
            log.info("[{}] Email notification sent to EmailService - OrderID: {}, Status: {}",
                    getCurrentTimestamp(),
                    delivery.getOrderId(),
                    delivery.getStatus());

        } catch (Exception e) {
            log.error("[{}] Failed to send status update notification - OrderID: {}, Error: {}",
                    getCurrentTimestamp(),
                    delivery.getOrderId(),
                    e.getMessage(),
                    e);
        }
    }

    private String generateStatusMessage(DeliveryStatus status) {
        switch (status) {
            case REQUEST_RECEIVED:
                return "Your delivery request has been received and is being processed.";
            case PICKED_UP:
                return "Your items have been picked up from the warehouse and are now at our delivery depot.";
            case IN_TRANSIT:
                return "Your package is on the delivery truck and on the way to you.";
            case DELIVERED:
                return "Your package has been successfully delivered. Thank you for your purchase!";
            case LOST:
                return "We apologize, but your package appears to be lost. Please contact customer service for assistance.";
            default:
                return "Delivery status updated.";
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DATE_FORMATTER);
    }
}
