package com.tut2.group3.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAvailabilityResponse {
    private Boolean available;
    private String fulfillmentStrategy; // SINGLE_WAREHOUSE, MULTIPLE_WAREHOUSES
    private List<WarehouseInfo> warehouses;
    private Integer requestedQuantity;
    private Integer totalAvailableQuantity;
}
