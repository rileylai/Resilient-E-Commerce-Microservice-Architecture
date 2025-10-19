package com.tut2.group3.store.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponseDto {

    private boolean success;
    private String message;
    private List<ProductDto> products;
    private double totalAmount;
}
