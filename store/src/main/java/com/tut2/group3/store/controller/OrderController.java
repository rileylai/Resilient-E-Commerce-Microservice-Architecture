package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.deliveryco.DeliverycoRequestDto;
import com.tut2.group3.store.dto.email.EmailRequestDto;
import com.tut2.group3.store.dto.order.OrderDto;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.service.BankService;
import com.tut2.group3.store.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //create order
    @PostMapping("/create")
    public Result createOrder(@RequestBody OrderDto orderDto) {
        orderService.createOrder(orderDto);
        return Result.success();
    }
    //show order
    @PostMapping("/{userId}/show")
    public Result showOrder(@PathVariable Long userId) {
        List<OrderDto> orderDtoList = orderService.showOrders(userId);
        return Result.success(orderDtoList);
    }

    //update order
    @PostMapping("/update")
    public Result updateOrder(@RequestBody OrderDto orderDto) {
        return Result.success();
    }
    //delete order
    @PostMapping("/delete")
    public Result deleteOrder(@RequestBody OrderDto orderDto) {
        return Result.success();
    }
    //pay for order
    @PostMapping("/{orderId}/pay")
    public void payOrder(@PathVariable Long orderId, BankRequestDto bankRequestDto) {
        //pay
    }
    //delivery item
    @PostMapping("/{orderId}/delivery")
    public void deliveryOrder(@PathVariable Long orderId, DeliverycoRequestDto deliverycoRequestDto) {

    }
    //notify user with email
    @PostMapping("/{orderId}/email")
    public void notifyUser(@PathVariable Long orderId, @RequestBody EmailRequestDto emailRequestDto) {

    }





}
