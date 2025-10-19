package com.tut2.group3.store.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseRequestDto {
    private Long orderId;
    private List<ProductDto> products;
}