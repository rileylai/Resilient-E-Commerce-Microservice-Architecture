package com.tut2.group3.store.service.serviceImpl;

import com.tut2.group3.store.dto.order.OrderDto;
import com.tut2.group3.store.dto.order.OrderItemDto;
import com.tut2.group3.store.mapper.OrderItemMapper;
import com.tut2.group3.store.mapper.OrderMapper;
import com.tut2.group3.store.pojo.Order;
import com.tut2.group3.store.pojo.OrderItem;
import com.tut2.group3.store.service.BankService;
import com.tut2.group3.store.service.OrderService;
import com.tut2.group3.store.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    @Autowired
    WarehouseService warehouseService;
    @Autowired
    BankService bankService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(OrderDto orderDto) {

        float totalAmount = 0;
        List<OrderItem> orderItems = new ArrayList<>();
        for(OrderItemDto itemDto: orderDto.getItems()){
            //need to get products from warehouseService first
            // price for mock
            float mockPrice = 100;
            totalAmount += mockPrice * itemDto.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(itemDto.getProductId());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(mockPrice);
            orderItems.add(orderItem);
        }

        //create order
        Order order = new Order();
        order.setStatus("Pending");
        order.setUserId(orderDto.getUserId());
        order.setTotalAmount(totalAmount);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

    }
}

