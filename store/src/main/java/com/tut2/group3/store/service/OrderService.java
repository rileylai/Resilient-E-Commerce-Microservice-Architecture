package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.pojo.Result;
import org.springframework.web.bind.annotation.RequestBody;

public interface OrderService {
    Result orderPlace(@RequestBody OrderCreateRequestDTO orderCreateRequestDTO);

    Result payOrder(Long orderID, Long userID, float amount, String currency);
}
