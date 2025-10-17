package com.tut2.group3.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseInfo {
    private Long warehouseId;
    private String warehouseName;
    private Integer availableQuantity;
    private Integer allocatedQuantity;
}
