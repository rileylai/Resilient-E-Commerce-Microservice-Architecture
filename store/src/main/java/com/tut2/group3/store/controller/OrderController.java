package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public void orderPlace(@RequestBody OrderCreateRequestDTO orderCreateRequestDTO) {

        orderService.orderPlace(orderCreateRequestDTO);

    }



}
