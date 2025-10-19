package com.tut2.group3.store.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long id;
    @NotNull
    private Long userId;
    private String status;
    private float totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<OrderItemDto> items;

}
