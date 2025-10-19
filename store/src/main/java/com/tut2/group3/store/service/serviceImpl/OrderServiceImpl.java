package com.tut2.group3.store.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tut2.group3.store.dto.order.OrderDto;
import com.tut2.group3.store.dto.order.OrderItemDto;
import com.tut2.group3.store.mapper.OrderItemMapper;
import com.tut2.group3.store.mapper.OrderMapper;
import com.tut2.group3.store.pojo.Order;
import com.tut2.group3.store.pojo.OrderItem;
import com.tut2.group3.store.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    // connect to third-party service
    @Autowired
    WarehouseService warehouseService;
    @Autowired
    BankService bankService;
    @Autowired
    EmailService emailService;
    @Autowired
    DeliverycoService deliverycoService;

    //create order method
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

    //show order with userId method
    @Override
    public List<OrderDto> showOrders(Long userId) {

        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>().eq("user_id", userId));
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        // 提取订单ID列表
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());

        // 查询所有订单项
        List<OrderItem> allItems = orderItemMapper.selectList(new QueryWrapper<OrderItem>().in("order_id", orderIds));

        // 按订单ID分组订单项
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        // 构造 DTO 列表
        List<OrderDto> result = new ArrayList<>();
        for (Order order : orders) {
            OrderDto dto = new OrderDto();
            dto.setId(order.getId());
            dto.setUserId(order.getUserId());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCreateTime(order.getCreateTime());
            dto.setUpdateTime(order.getUpdateTime());

            List<OrderItemDto> itemDtoList = itemMap.getOrDefault(order.getId(), Collections.emptyList())
                    .stream()
                    .map(item -> {
                        OrderItemDto itemDto = new OrderItemDto();
                        itemDto.setProductId(item.getProductId());
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setPrice(item.getPrice());
                        return itemDto;
                    })
                    .collect(Collectors.toList());

            dto.setItems(itemDtoList);
            result.add(dto);
        }

        return result;

    }


}

