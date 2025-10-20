package com.tut2.group3.deliveryco.controller;

import com.auth0.jwt.interfaces.Claim;
import com.tut2.group3.deliveryco.common.ErrorCode;
import com.tut2.group3.deliveryco.entity.Delivery;
import com.tut2.group3.deliveryco.entity.Result;
import com.tut2.group3.deliveryco.entity.enums.DeliveryStatus;
import com.tut2.group3.deliveryco.service.DeliveryService;
import com.tut2.group3.deliveryco.util.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Delivery Controller
 *
 * REST API endpoints for querying and managing deliveries.
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @GetMapping("/health")
    public Result<String> health() {
        log.info("Health check endpoint called");
        return Result.success("DeliveryCo service is running");
    }

    @GetMapping("/order/{orderId}")
    public Result<Delivery> getDeliveryByOrderId(@PathVariable Long orderId) {
        log.info("Getting delivery for OrderID: {}", orderId);

        Delivery delivery = deliveryService.getDeliveryByOrderId(orderId);
        if (delivery == null) {
            log.warn("Delivery not found for OrderID: {}", orderId);
            return Result.error(ErrorCode.DELIVERY_NOT_FOUND, "No delivery found for order ID: " + orderId);
        }

        return Result.success(delivery);
    }

    @GetMapping("/{deliveryId}")
    public Result<Delivery> getDeliveryById(@PathVariable Long deliveryId) {
        log.info("Getting delivery by DeliveryID: {}", deliveryId);

        Delivery delivery = deliveryService.getDeliveryById(deliveryId);
        if (delivery == null) {
            log.warn("Delivery not found - DeliveryID: {}", deliveryId);
            return Result.error(ErrorCode.DELIVERY_NOT_FOUND, "Delivery not found with ID: " + deliveryId);
        }

        return Result.success(delivery);
    }

    @GetMapping("/my-deliveries")
    public Result<List<Delivery>> getMyDeliveries() {
        // Get customer ID from JWT claims
        Map<String, Claim> claims = ThreadLocalUtil.get();
        Long customerId = claims.get("userId").asLong();

        log.info("Getting all deliveries for CustomerID: {}", customerId);

        List<Delivery> deliveries = deliveryService.getDeliveriesByCustomerId(customerId);
        return Result.success(deliveries);
    }

    @GetMapping("/customer/{customerId}")
    public Result<List<Delivery>> getDeliveriesByCustomerId(@PathVariable Long customerId) {
        log.info("Getting deliveries for CustomerID: {}", customerId);

        List<Delivery> deliveries = deliveryService.getDeliveriesByCustomerId(customerId);
        return Result.success(deliveries);
    }

    @GetMapping("/status/{status}")
    public Result<List<Delivery>> getDeliveriesByStatus(@PathVariable String status) {
        log.info("Getting deliveries with status: {}", status);

        try {
            DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
            List<Delivery> deliveries = deliveryService.getDeliveriesByStatus(deliveryStatus);
            return Result.success(deliveries);
        } catch (IllegalArgumentException e) {
            log.error("Invalid delivery status: {}", status);
            return Result.error(ErrorCode.BAD_REQUEST, "Invalid delivery status: " + status);
        }
    }

    @GetMapping("/all")
    public Result<List<Delivery>> getAllDeliveries() {
        log.info("Getting all deliveries");

        List<Delivery> deliveries = deliveryService.getAllDeliveries();
        return Result.success(deliveries);
    }

    @PutMapping("/{deliveryId}/status")
    public Result<Delivery> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestBody Map<String, String> request) {

        String statusStr = request.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return Result.error(ErrorCode.BAD_REQUEST, "Status is required");
        }

        log.info("Manually updating delivery status - DeliveryID: {}, NewStatus: {}", deliveryId, statusStr);

        try {
            DeliveryStatus newStatus = DeliveryStatus.valueOf(statusStr.toUpperCase());

            Delivery delivery = deliveryService.getDeliveryById(deliveryId);
            if (delivery == null) {
                return Result.error(ErrorCode.DELIVERY_NOT_FOUND, "Delivery not found with ID: " + deliveryId);
            }

            // Check if already in final state
            if (delivery.getStatus() == DeliveryStatus.DELIVERED || delivery.getStatus() == DeliveryStatus.LOST) {
                return Result.error(ErrorCode.DELIVERY_ALREADY_COMPLETED,
                        "Cannot update delivery in final status: " + delivery.getStatus());
            }

            Delivery updatedDelivery = deliveryService.updateDeliveryStatus(deliveryId, newStatus);
            return Result.success("Delivery status updated successfully", updatedDelivery);

        } catch (IllegalArgumentException e) {
            log.error("Invalid delivery status: {}", statusStr);
            return Result.error(ErrorCode.BAD_REQUEST, "Invalid delivery status: " + statusStr);
        } catch (Exception e) {
            log.error("Error updating delivery status", e);
            return Result.error(ErrorCode.STATUS_UPDATE_FAILED, "Failed to update delivery status: " + e.getMessage());
        }
    }

    @GetMapping("/{deliveryId}/status")
    public Result<Map<String, Object>> getDeliveryStatus(@PathVariable Long deliveryId) {
        log.info("Getting status for DeliveryID: {}", deliveryId);

        Delivery delivery = deliveryService.getDeliveryById(deliveryId);
        if (delivery == null) {
            return Result.error(ErrorCode.DELIVERY_NOT_FOUND, "Delivery not found with ID: " + deliveryId);
        }

        Map<String, Object> statusInfo = Map.of(
                "deliveryId", delivery.getId(),
                "orderId", delivery.getOrderId(),
                "status", delivery.getStatus(),
                "updatedAt", delivery.getUpdatedAt()
        );

        return Result.success(statusInfo);
    }
}
