package com.tut2.group3.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStockResponse {
    private Long warehouseId;
    private Long productId;
    private Integer previousAvailableQuantity;
    private Integer newAvailableQuantity;
    private String operation;
    private LocalDateTime timestamp;
}
