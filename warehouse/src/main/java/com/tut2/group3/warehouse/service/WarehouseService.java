package com.tut2.group3.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tut2.group3.warehouse.common.ErrorCode;
import com.tut2.group3.warehouse.dto.request.*;
import com.tut2.group3.warehouse.dto.response.*;
import com.tut2.group3.warehouse.entity.Inventory;
import com.tut2.group3.warehouse.entity.Product;
import com.tut2.group3.warehouse.entity.StockReservation;
import com.tut2.group3.warehouse.entity.Warehouse;
import com.tut2.group3.warehouse.mapper.InventoryMapper;
import com.tut2.group3.warehouse.mapper.ProductMapper;
import com.tut2.group3.warehouse.mapper.StockReservationMapper;
import com.tut2.group3.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final InventoryMapper inventoryMapper;
    private final StockReservationMapper stockReservationMapper;
    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE_NAME = "warehouse.exchange";
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public StockAvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {
        log.info("Checking stock availability for product {} with quantity {}",
                request.getProductId(), request.getQuantity());

        // Verify product exists
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }

        // Query all active warehouses with available stock for this product
        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getProductId, request.getProductId())
                   .gt(Inventory::getAvailableQuantity, 0);

        List<Inventory> inventories = inventoryMapper.selectList(queryWrapper);

        if (inventories.isEmpty()) {
            return StockAvailabilityResponse.builder()
                    .available(false)
                    .requestedQuantity(request.getQuantity())
                    .totalAvailableQuantity(0)
                    .build();
        }

        // Calculate total available quantity
        int totalAvailable = inventories.stream()
                .mapToInt(Inventory::getAvailableQuantity)
                .sum();

        if (totalAvailable < request.getQuantity()) {
            return StockAvailabilityResponse.builder()
                    .available(false)
                    .requestedQuantity(request.getQuantity())
                    .totalAvailableQuantity(totalAvailable)
                    .build();
        }

        // Allocate stock from warehouses
        List<WarehouseInfo> warehouseInfos = allocateStock(inventories, request.getQuantity());
        String strategy = warehouseInfos.size() == 1 ? "SINGLE_WAREHOUSE" : "MULTIPLE_WAREHOUSES";

        return StockAvailabilityResponse.builder()
                .available(true)
                .fulfillmentStrategy(strategy)
                .warehouses(warehouseInfos)
                .requestedQuantity(request.getQuantity())
                .totalAvailableQuantity(totalAvailable)
                .build();
    }

    /**
     * Allocate stock from warehouses using greedy algorithm
     */
    private List<WarehouseInfo> allocateStock(List<Inventory> inventories, int requiredQuantity) {
        List<WarehouseInfo> allocations = new ArrayList<>();
        int remaining = requiredQuantity;

        // Sort by available quantity descending to minimize warehouse count
        inventories.sort((a, b) -> b.getAvailableQuantity().compareTo(a.getAvailableQuantity()));

        for (Inventory inventory : inventories) {
            if (remaining <= 0) break;

            Warehouse warehouse = warehouseMapper.selectById(inventory.getWarehouseId());
            if (warehouse == null || !"ACTIVE".equals(warehouse.getStatus())) {
                continue;
            }

            int allocated = Math.min(inventory.getAvailableQuantity(), remaining);
            allocations.add(WarehouseInfo.builder()
                    .warehouseId(warehouse.getId())
                    .warehouseName(warehouse.getName())
                    .availableQuantity(inventory.getAvailableQuantity())
                    .allocatedQuantity(allocated)
                    .build());

            remaining -= allocated;
        }

        return allocations;
    }

    @Transactional(rollbackFor = Exception.class)
    public StockReservationResponse reserveStock(ReserveStockRequest request) {
        log.info("Reserving stock for order {}", request.getOrderId());

        // Verify product exists
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new RuntimeException(ErrorCode.PRODUCT_NOT_FOUND.getMessage());
        }

        // Validate total quantity matches
        int totalAllocated = request.getWarehouses().stream()
                .mapToInt(WarehouseAllocation::getQuantity)
                .sum();

        if (totalAllocated != request.getQuantity()) {
            throw new RuntimeException("Total allocated quantity does not match requested quantity");
        }

        String reservationId = "RES-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        List<ReservationDetail> reservationDetails = new ArrayList<>();

        // Reserve stock from each warehouse
        for (WarehouseAllocation allocation : request.getWarehouses()) {
            boolean reserved = reserveStockFromWarehouse(
                    request.getOrderId(),
                    allocation.getWarehouseId(),
                    request.getProductId(),
                    allocation.getQuantity()
            );

            if (!reserved) {
                throw new RuntimeException(ErrorCode.STOCK_RESERVATION_FAILED.getMessage() +
                        " at warehouse " + allocation.getWarehouseId());
            }

            Warehouse warehouse = warehouseMapper.selectById(allocation.getWarehouseId());
            reservationDetails.add(ReservationDetail.builder()
                    .warehouseId(allocation.getWarehouseId())
                    .warehouseName(warehouse.getName())
                    .productId(request.getProductId())
                    .quantity(allocation.getQuantity())
                    .status("RESERVED")
                    .build());
        }

        // Publish stock reserved event
        publishStockReservedEvent(request.getOrderId(), reservationId, request.getProductId(),
                request.getQuantity(), request.getWarehouses());

        return StockReservationResponse.builder()
                .orderId(request.getOrderId())
                .reservationId(reservationId)
                .reservations(reservationDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Reserve stock from a specific warehouse with retry logic
     */
    private boolean reserveStockFromWarehouse(String orderId, Long warehouseId, Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            // Get current inventory with version
            LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Inventory::getWarehouseId, warehouseId)
                       .eq(Inventory::getProductId, productId);

            Inventory inventory = inventoryMapper.selectOne(queryWrapper);
            if (inventory == null) {
                log.error("Inventory not found for warehouse {} and product {}", warehouseId, productId);
                return false;
            }

            if (inventory.getAvailableQuantity() < quantity) {
                log.error("Insufficient stock at warehouse {}. Available: {}, Required: {}",
                        warehouseId, inventory.getAvailableQuantity(), quantity);
                return false;
            }

            // Try to reserve with optimistic locking
            int updated = inventoryMapper.reserveStock(warehouseId, productId, quantity, inventory.getVersion());

            if (updated > 0) {
                // Create reservation record
                StockReservation reservation = new StockReservation();
                reservation.setOrderId(orderId);
                reservation.setWarehouseId(warehouseId);
                reservation.setProductId(productId);
                reservation.setQuantity(quantity);
                reservation.setStatus("RESERVED");
                stockReservationMapper.insert(reservation);

                log.info("Successfully reserved {} units from warehouse {} for order {}",
                        quantity, warehouseId, orderId);
                return true;
            }

            // Optimistic lock failed, retry
            log.warn("Optimistic lock failed for warehouse {}, attempt {}/{}",
                    warehouseId, attempt + 1, MAX_RETRY_ATTEMPTS);

            try {
                Thread.sleep(50 * (attempt + 1)); // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    /**
     * Confirm stock reservation after payment
     */
    @Transactional(rollbackFor = Exception.class)
    public ConfirmReservationResponse confirmReservation(ConfirmReservationRequest request) {
        log.info("Confirming reservation for order {}", request.getOrderId());

        // Find all reservations for this order
        LambdaQueryWrapper<StockReservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StockReservation::getOrderId, request.getOrderId())
                   .eq(StockReservation::getStatus, "RESERVED");

        List<StockReservation> reservations = stockReservationMapper.selectList(queryWrapper);

        if (reservations.isEmpty()) {
            throw new RuntimeException(ErrorCode.RESERVATION_NOT_FOUND.getMessage());
        }

        List<ReservationDetail> details = new ArrayList<>();

        for (StockReservation reservation : reservations) {
            // Update reservation status
            reservation.setStatus("CONFIRMED");
            stockReservationMapper.updateById(reservation);

            // Confirm in inventory (deduct from reserved quantity)
            confirmReservationInInventory(reservation.getWarehouseId(),
                    reservation.getProductId(), reservation.getQuantity());

            Warehouse warehouse = warehouseMapper.selectById(reservation.getWarehouseId());
            details.add(ReservationDetail.builder()
                    .warehouseId(reservation.getWarehouseId())
                    .warehouseName(warehouse.getName())
                    .productId(reservation.getProductId())
                    .quantity(reservation.getQuantity())
                    .status("CONFIRMED")
                    .build());
        }

        // Publish stock confirmed event
        publishStockConfirmedEvent(request.getOrderId(), request.getReservationId(), details);

        return ConfirmReservationResponse.builder()
                .orderId(request.getOrderId())
                .reservationId(request.getReservationId())
                .status("CONFIRMED")
                .warehouses(details)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Confirm reservation in inventory
     */
    private void confirmReservationInInventory(Long warehouseId, Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Inventory::getWarehouseId, warehouseId)
                       .eq(Inventory::getProductId, productId);

            Inventory inventory = inventoryMapper.selectOne(queryWrapper);
            if (inventory == null) {
                throw new RuntimeException("Inventory not found");
            }

            int updated = inventoryMapper.confirmReservation(warehouseId, productId, quantity, inventory.getVersion());

            if (updated > 0) {
                return;
            }

            try {
                Thread.sleep(50 * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to confirm reservation");
            }
        }

        throw new RuntimeException("Failed to confirm reservation after retries");
    }

    /**
     * Release reserved stock
     */
    @Transactional(rollbackFor = Exception.class)
    public ReleaseStockResponse releaseStock(ReleaseStockRequest request) {
        log.info("Releasing stock for order {}, reason: {}", request.getOrderId(), request.getReason());

        // Find all reservations for this order
        LambdaQueryWrapper<StockReservation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StockReservation::getOrderId, request.getOrderId())
                   .eq(StockReservation::getStatus, "RESERVED");

        List<StockReservation> reservations = stockReservationMapper.selectList(queryWrapper);

        if (reservations.isEmpty()) {
            throw new RuntimeException(ErrorCode.RESERVATION_NOT_FOUND.getMessage());
        }

        List<ReleaseStockResponse.WarehouseReleaseInfo> releaseInfos = new ArrayList<>();

        for (StockReservation reservation : reservations) {
            // Update reservation status
            reservation.setStatus("RELEASED");
            stockReservationMapper.updateById(reservation);

            // Release stock in inventory
            releaseStockInInventory(reservation.getWarehouseId(),
                    reservation.getProductId(), reservation.getQuantity());

            releaseInfos.add(ReleaseStockResponse.WarehouseReleaseInfo.builder()
                    .warehouseId(reservation.getWarehouseId())
                    .productId(reservation.getProductId())
                    .quantity(reservation.getQuantity())
                    .releasedAt(LocalDateTime.now())
                    .build());
        }

        // Publish stock released event
        publishStockReleasedEvent(request.getOrderId(), request.getReservationId(), request.getReason());

        return ReleaseStockResponse.builder()
                .orderId(request.getOrderId())
                .reservationId(request.getReservationId())
                .status("RELEASED")
                .warehouses(releaseInfos)
                .build();
    }

    /**
     * Release stock in inventory
     */
    private void releaseStockInInventory(Long warehouseId, Long productId, int quantity) {
        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Inventory::getWarehouseId, warehouseId)
                       .eq(Inventory::getProductId, productId);

            Inventory inventory = inventoryMapper.selectOne(queryWrapper);
            if (inventory == null) {
                throw new RuntimeException("Inventory not found");
            }

            int updated = inventoryMapper.releaseStock(warehouseId, productId, quantity, inventory.getVersion());

            if (updated > 0) {
                return;
            }

            try {
                Thread.sleep(50 * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to release stock");
            }
        }

        throw new RuntimeException("Failed to release stock after retries");
    }

    /**
     * Get warehouse stock information
     */
    public WarehouseStockResponse getWarehouseStock(Long warehouseId, Long productId, int page, int size) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null) {
            throw new RuntimeException(ErrorCode.WAREHOUSE_NOT_FOUND.getMessage());
        }

        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getWarehouseId, warehouseId);

        if (productId != null) {
            queryWrapper.eq(Inventory::getProductId, productId);
        }

        Page<Inventory> pageRequest = new Page<>(page, size);
        Page<Inventory> inventoryPage = inventoryMapper.selectPage(pageRequest, queryWrapper);

        List<StockInfo> stockInfos = new ArrayList<>();
        for (Inventory inventory : inventoryPage.getRecords()) {
            Product product = productMapper.selectById(inventory.getProductId());
            stockInfos.add(StockInfo.builder()
                    .productId(inventory.getProductId())
                    .productName(product != null ? product.getName() : "Unknown")
                    .availableQuantity(inventory.getAvailableQuantity())
                    .reservedQuantity(inventory.getReservedQuantity())
                    .totalQuantity(inventory.getAvailableQuantity() + inventory.getReservedQuantity())
                    .build());
        }

        return WarehouseStockResponse.builder()
                .warehouseId(warehouseId)
                .warehouseName(warehouse.getName())
                .stocks(stockInfos)
                .pagination(WarehouseStockResponse.PaginationInfo.builder()
                        .page(page)
                        .size(size)
                        .totalElements(inventoryPage.getTotal())
                        .totalPages((int) inventoryPage.getPages())
                        .build())
                .build();
    }

    /**
     * Update stock level
     */
    @Transactional(rollbackFor = Exception.class)
    public UpdateStockResponse updateStock(UpdateStockRequest request) {
        log.info("Updating stock for warehouse {} product {}: {} {}",
                request.getWarehouseId(), request.getProductId(), request.getOperation(), request.getQuantity());

        LambdaQueryWrapper<Inventory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Inventory::getWarehouseId, request.getWarehouseId())
                   .eq(Inventory::getProductId, request.getProductId());

        Inventory inventory = inventoryMapper.selectOne(queryWrapper);

        // If inventory doesn't exist, create it
        if (inventory == null) {
            inventory = new Inventory();
            inventory.setWarehouseId(request.getWarehouseId());
            inventory.setProductId(request.getProductId());
            inventory.setAvailableQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setVersion(0);
            inventoryMapper.insert(inventory);
        }

        int previousQuantity = inventory.getAvailableQuantity();
        int newQuantity;

        switch (request.getOperation().toUpperCase()) {
            case "ADD":
                newQuantity = previousQuantity + request.getQuantity();
                break;
            case "SET":
                newQuantity = request.getQuantity();
                break;
            case "SUBTRACT":
                newQuantity = previousQuantity - request.getQuantity();
                if (newQuantity < 0) {
                    throw new RuntimeException(ErrorCode.INVALID_QUANTITY.getMessage());
                }
                break;
            default:
                throw new RuntimeException("Invalid operation: " + request.getOperation());
        }

        inventory.setAvailableQuantity(newQuantity);
        inventoryMapper.updateById(inventory);

        return UpdateStockResponse.builder()
                .warehouseId(request.getWarehouseId())
                .productId(request.getProductId())
                .previousAvailableQuantity(previousQuantity)
                .newAvailableQuantity(newQuantity)
                .operation(request.getOperation())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Get all warehouses
     */
    public List<WarehouseDTO> getAllWarehouses(String status) {
        LambdaQueryWrapper<Warehouse> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Warehouse::getStatus, status);
        }

        return warehouseMapper.selectList(queryWrapper).stream()
                .map(w -> WarehouseDTO.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .address(w.getAddress())
                        .status(w.getStatus())
                        .build())
                .toList();
    }

    // Event publishing methods
    private void publishStockReservedEvent(String orderId, String reservationId, Long productId,
                                          int quantity, List<WarehouseAllocation> warehouses) {
        try {
            String eventId = "EVT-" + System.currentTimeMillis();
            String message = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"STOCK_RESERVED\",\"timestamp\":\"%s\"," +
                "\"orderId\":\"%s\",\"reservationId\":\"%s\",\"productId\":%d,\"quantity\":%d}",
                eventId, LocalDateTime.now(), orderId, reservationId, productId, quantity
            );
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "warehouse.stock.reserved", message);
            log.info("Published STOCK_RESERVED event for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish STOCK_RESERVED event", e);
        }
    }

    private void publishStockConfirmedEvent(String orderId, String reservationId, List<ReservationDetail> details) {
        try {
            String eventId = "EVT-" + System.currentTimeMillis();
            String message = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"STOCK_CONFIRMED\",\"timestamp\":\"%s\"," +
                "\"orderId\":\"%s\",\"reservationId\":\"%s\"}",
                eventId, LocalDateTime.now(), orderId, reservationId
            );
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "warehouse.stock.confirmed", message);
            log.info("Published STOCK_CONFIRMED event for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish STOCK_CONFIRMED event", e);
        }
    }

    private void publishStockReleasedEvent(String orderId, String reservationId, String reason) {
        try {
            String eventId = "EVT-" + System.currentTimeMillis();
            String message = String.format(
                "{\"eventId\":\"%s\",\"eventType\":\"STOCK_RELEASED\",\"timestamp\":\"%s\"," +
                "\"orderId\":\"%s\",\"reservationId\":\"%s\",\"reason\":\"%s\"}",
                eventId, LocalDateTime.now(), orderId, reservationId, reason
            );
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "warehouse.stock.released", message);
            log.info("Published STOCK_RELEASED event for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish STOCK_RELEASED event", e);
        }
    }
}
