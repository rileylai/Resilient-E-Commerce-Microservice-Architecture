package com.tut2.group3.store.client;

import com.tut2.group3.store.dto.warehouse.*;
import com.tut2.group3.store.pojo.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "warehouse", url = "http://localhost:8081")
public interface WarehouseClient {

    // ============= Product Management Endpoints =============

    /**
     * Get all products
     */
    @GetMapping("/api/products")
    Result<List<ProductDto>> getAllProducts();

    /**
     * Get product price by ID or name
     */
    @GetMapping("/api/products/price")
    Result<ProductPriceResponse> getProductPrice(@RequestParam(required = false) Long id,
                                                  @RequestParam(required = false) String name);

    // ============= Warehouse Management Endpoints =============

    /**
     * Validate order - check if all products in the order can be fulfilled
     */
    @PostMapping("/api/warehouse/validate-order")
    Result<OrderValidationResponse> validateOrder(@Valid @RequestBody OrderValidationRequest request);

    /**
     * Check stock availability
     */
    @PostMapping("/api/warehouse/check-availability")
    Result<StockAvailabilityResponse> checkAvailability(@Valid @RequestBody CheckAvailabilityRequest request);

    /**
     * Reserve stock for an order
     */
    @PostMapping("/api/warehouse/reserve")
    Result<StockReservationResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request);

    /**
     * Confirm stock reservation after payment
     */
    @PostMapping("/api/warehouse/confirm")
    Result<ConfirmReservationResponse> confirmReservation(@Valid @RequestBody ConfirmReservationRequest request);

    /**
     * Release reserved stock
     */
    @PostMapping("/api/warehouse/release")
    Result<ReleaseStockResponse> releaseStock(@Valid @RequestBody ReleaseStockRequest request);

    /**
     * Get warehouse stock information
     */
    @GetMapping("/api/warehouse/{warehouseId}/stock")
    Result<WarehouseStockResponse> getWarehouseStock(@PathVariable("warehouseId") Long warehouseId,
                                                      @RequestParam(required = false) Long productId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size);

    /**
     * Update stock level
     */
    @PutMapping("/api/warehouse/stock")
    Result<UpdateStockResponse> updateStock(@Valid @RequestBody UpdateStockRequest request);

    /**
     * Get all warehouses
     */
    @GetMapping("/api/warehouse/list")
    Result<List<WarehouseDTO>> getAllWarehouses(@RequestParam(required = false) String status);

    /**
     * Health check endpoint
     */
    @GetMapping("/api/warehouse/health")
    Result<HealthCheckResponse> healthCheck();
}
