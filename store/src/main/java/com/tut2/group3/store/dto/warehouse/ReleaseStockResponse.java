package com.tut2.group3.store.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseStockResponse {
    private String orderId;
    private String reservationId;
    private String status;
    private List<WarehouseReleaseInfo> warehouses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WarehouseReleaseInfo {
        private Long warehouseId;
        private Long productId;
        private Integer quantity;
        private LocalDateTime releasedAt;
    }
}

