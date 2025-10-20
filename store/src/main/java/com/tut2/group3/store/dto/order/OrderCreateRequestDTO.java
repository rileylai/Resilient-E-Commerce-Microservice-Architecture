package com.tut2.group3.store.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDTO {

    private Long userId;
    private List<OrderItemRequestDTO> items;
}
