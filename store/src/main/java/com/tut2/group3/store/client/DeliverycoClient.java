package com.tut2.group3.store.client;

import com.tut2.group3.store.dto.deliveryco.DeliveryDto;
import com.tut2.group3.store.dto.deliveryco.DeliveryStatusDto;
import com.tut2.group3.store.dto.deliveryco.UpdateDeliveryStatusRequest;
import com.tut2.group3.store.pojo.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "deliveryco", url = "http://localhost:8083")
public interface DeliverycoClient {

    /**
     * Health check endpoint
     */
    @GetMapping("/api/delivery/health")
    Result<String> health();

    /**
     * Get delivery by order ID
     */
    @GetMapping("/api/delivery/order/{orderId}")
    Result<DeliveryDto> getDeliveryByOrderId(@PathVariable("orderId") Long orderId);

    /**
     * Get delivery by delivery ID
     */
    @GetMapping("/api/delivery/{deliveryId}")
    Result<DeliveryDto> getDeliveryById(@PathVariable("deliveryId") Long deliveryId);

    /**
     * Get all deliveries for a customer
     */
    @GetMapping("/api/delivery/customer/{customerId}")
    Result<List<DeliveryDto>> getDeliveriesByCustomerId(@PathVariable("customerId") Long customerId);

    /**
     * Get deliveries by status
     */
    @GetMapping("/api/delivery/status/{status}")
    Result<List<DeliveryDto>> getDeliveriesByStatus(@PathVariable("status") String status);

    /**
     * Get all deliveries
     */
    @GetMapping("/api/delivery/all")
    Result<List<DeliveryDto>> getAllDeliveries();

    /**
     * Update delivery status
     */
    @PutMapping("/api/delivery/{deliveryId}/status")
    Result<DeliveryDto> updateDeliveryStatus(@PathVariable("deliveryId") Long deliveryId,
                                              @RequestBody UpdateDeliveryStatusRequest request);

    /**
     * Get delivery status
     */
    @GetMapping("/api/delivery/{deliveryId}/status")
    Result<DeliveryStatusDto> getDeliveryStatus(@PathVariable("deliveryId") Long deliveryId);
}

