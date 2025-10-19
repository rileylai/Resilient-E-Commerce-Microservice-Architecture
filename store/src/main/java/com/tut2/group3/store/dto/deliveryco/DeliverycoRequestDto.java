package com.tut2.group3.store.dto.deliveryco;

import com.tut2.group3.store.dto.order.OrderDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliverycoRequestDto {
    private Long orderId;
    private Long userId;
    private List<OrderDto> orderDtoList;
}
