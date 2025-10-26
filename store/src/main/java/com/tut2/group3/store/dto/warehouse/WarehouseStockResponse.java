package com.tut2.group3.store.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStockResponse {
    private Long warehouseId;
    private String warehouseName;
    private List<StockInfo> stocks;
    private PaginationInfo pagination;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationInfo {
        private Integer page;
        private Integer size;
        private Long totalElements;
        private Integer totalPages;
    }
}

