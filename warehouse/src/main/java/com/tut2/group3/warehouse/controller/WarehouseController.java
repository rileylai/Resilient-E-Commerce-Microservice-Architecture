package com.tut2.group3.warehouse.controller;

import com.tut2.group3.warehouse.common.ErrorCode;
import com.tut2.group3.warehouse.common.Result;
import com.tut2.group3.warehouse.dto.request.*;
import com.tut2.group3.warehouse.dto.response.*;
import com.tut2.group3.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@Validated
@Slf4j
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * Validate order - check if all products in the order can be fulfilled
     */
    @PostMapping("/validate-order")
    public Result<OrderValidationResponse> validateOrder(
            @Valid @RequestBody OrderValidationRequest request) {
        try {
            OrderValidationResponse response = warehouseService.validateOrder(request);

            if (response.getValid()) {
                return Result.success(response.getMessage(), response);
            } else {
                return Result.error(
                        ErrorCode.INSUFFICIENT_STOCK,
                        response.getMessage(),
                        response
                );
            }
        } catch (Exception e) {
            log.error("Error validating order {}", request.getOrderId(), e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Check stock availability
     */
    @PostMapping("/check-availability")
    public Result<StockAvailabilityResponse> checkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request) {
        try {
            StockAvailabilityResponse response = warehouseService.checkAvailability(request);

            if (response.getAvailable()) {
                String message = response.getFulfillmentStrategy().equals("SINGLE_WAREHOUSE")
                        ? "Stock is available"
                        : "Stock is available from multiple warehouses";
                return Result.success(message, response);
            } else {
                return Result.error(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "Insufficient stock available",
                        response
                );
            }
        } catch (Exception e) {
            log.error("Error checking stock availability", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Reserve stock for an order
     */
    @PostMapping("/reserve")
    public Result<StockReservationResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        try {
            StockReservationResponse response = warehouseService.reserveStock(request);
            return Result.success("Stock reserved successfully", response);
        } catch (RuntimeException e) {
            log.error("Error reserving stock for order {}", request.getOrderId(), e);
            return Result.error(
                    ErrorCode.STOCK_RESERVATION_FAILED,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error reserving stock", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Confirm stock reservation after payment
     */
    @PostMapping("/confirm")
    public Result<ConfirmReservationResponse> confirmReservation(
            @Valid @RequestBody ConfirmReservationRequest request) {
        try {
            ConfirmReservationResponse response = warehouseService.confirmReservation(request);
            return Result.success("Stock reservation confirmed", response);
        } catch (RuntimeException e) {
            log.error("Error confirming reservation for order {}", request.getOrderId(), e);
            return Result.error(
                    ErrorCode.RESERVATION_NOT_FOUND,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error confirming reservation", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Release reserved stock
     */
    @PostMapping("/release")
    public Result<ReleaseStockResponse> releaseStock(
            @Valid @RequestBody ReleaseStockRequest request) {
        try {
            ReleaseStockResponse response = warehouseService.releaseStock(request);
            return Result.success("Stock released successfully", response);
        } catch (RuntimeException e) {
            log.error("Error releasing stock for order {}", request.getOrderId(), e);
            return Result.error(
                    ErrorCode.RESERVATION_NOT_FOUND,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error releasing stock", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Get warehouse stock information
     */
    @GetMapping("/{warehouseId}/stock")
    public Result<WarehouseStockResponse> getWarehouseStock(
            @PathVariable Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            WarehouseStockResponse response = warehouseService.getWarehouseStock(
                    warehouseId, productId, page, size);
            return Result.success("Stock information retrieved", response);
        } catch (RuntimeException e) {
            log.error("Error getting warehouse stock", e);
            return Result.error(
                    ErrorCode.WAREHOUSE_NOT_FOUND,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error getting warehouse stock", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Update stock level
     */
    @PutMapping("/stock")
    public Result<UpdateStockResponse> updateStock(
            @Valid @RequestBody UpdateStockRequest request) {
        try {
            UpdateStockResponse response = warehouseService.updateStock(request);
            return Result.success("Stock level updated", response);
        } catch (RuntimeException e) {
            log.error("Error updating stock", e);
            return Result.error(
                    ErrorCode.STOCK_UPDATE_FAILED,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error updating stock", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Get all warehouses
     */
    @GetMapping("/list")
    public Result<List<WarehouseDTO>> getAllWarehouses(
            @RequestParam(required = false) String status) {
        try {
            List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses(status);
            return Result.success("Warehouses retrieved", warehouses);
        } catch (Exception e) {
            log.error("Error getting warehouses", e);
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    e.getMessage()
            );
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public Result<HealthCheckResponse> healthCheck() {
        try {
            HealthCheckResponse response = HealthCheckResponse.builder()
                    .status("UP")
                    .timestamp(LocalDateTime.now())
                    .database("CONNECTED")
                    .messageQueue("CONNECTED")
                    .build();
            return Result.success("Service is healthy", response);
        } catch (Exception e) {
            log.error("Health check failed", e);
            HealthCheckResponse response = HealthCheckResponse.builder()
                    .status("DOWN")
                    .timestamp(LocalDateTime.now())
                    .database("UNKNOWN")
                    .messageQueue("UNKNOWN")
                    .build();
            return Result.error(
                    ErrorCode.INTERNAL_ERROR,
                    "Service health check failed",
                    response
            );
        }
    }
}
