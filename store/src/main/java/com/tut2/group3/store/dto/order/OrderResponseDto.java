package com.tut2.group3.store.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private String status;
    private float totalAmount;
    private LocalDateTime createTime;
    private List<OrderItemDetailDto> items;
}
