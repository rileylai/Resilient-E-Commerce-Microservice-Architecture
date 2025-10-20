package com.tut2.group3.store.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDetailDto {
    private long productId;
    private String productName;
    private int quantity;
    private float price;
    private float subTotal;
}
