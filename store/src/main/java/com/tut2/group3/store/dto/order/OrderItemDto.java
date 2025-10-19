package com.tut2.group3.store.dto.order;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {

    private Long productId;
    @Positive(message = "the quantity must be positive")
    private Integer quantity;
    private float price;

}
