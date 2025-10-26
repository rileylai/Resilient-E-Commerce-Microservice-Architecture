package com.tut2.group3.store.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockInfo {
    private Long productId;
    private String productName;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
}

