package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.order.OrderDto;

import java.util.List;

public interface OrderService {

    void createOrder(OrderDto orderDto);

    List<OrderDto> showOrders(Long userId);
}
