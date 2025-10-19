package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.order.OrderDto;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result createOrder(@RequestBody OrderDto orderDto) {
        orderService.createOrder(orderDto);
        return Result.success();
    }


}
