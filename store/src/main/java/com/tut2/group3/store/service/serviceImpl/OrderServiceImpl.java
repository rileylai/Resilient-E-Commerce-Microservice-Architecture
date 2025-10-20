package com.tut2.group3.store.service.serviceImpl;

import com.tut2.group3.store.client.BankClient;
import com.tut2.group3.store.client.WarehouseClient;
import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.TransactionDto;
import com.tut2.group3.store.dto.order.OrderCreateRequestDTO;
import com.tut2.group3.store.dto.order.OrderItemDetailDto;
import com.tut2.group3.store.dto.order.OrderItemRequestDTO;
import com.tut2.group3.store.dto.order.OrderResponseDto;
import com.tut2.group3.store.dto.warehouse.ProductDto;
import com.tut2.group3.store.mapper.OrderItemMapper;
import com.tut2.group3.store.mapper.OrderMapper;
import com.tut2.group3.store.pojo.Order;
import com.tut2.group3.store.pojo.OrderItem;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final WarehouseClient warehouseClient;
    private final BankClient bankClient;

    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;

    @Override
    public Result orderPlace(OrderCreateRequestDTO orderCreateRequestDTO) {

        if(!warehouseClient.checkStock(orderCreateRequestDTO)){
            return Result.error(400,"Inventory shortage");
        }
        //warehouseClient.reserve();

        //create Order
        OrderResponseDto orderResponseDto = createOrder(orderCreateRequestDTO);

        //pay order

        payOrder(orderResponseDto.getOrderId(),orderCreateRequestDTO.getUserId(),orderResponseDto.getTotalAmount(),"AUD");


        return Result.success("Order created successfully. Awaiting delivery.");
    }

    @Override
    public Result payOrder(Long orderID, Long userID, float amount, String currency) {
        BankRequestDto bankRequestDto = new BankRequestDto();
        bankRequestDto.setOrderId(orderID.toString());
        bankRequestDto.setUserId(userID.toString());
        BigDecimal bd = new BigDecimal(String.valueOf(amount));
        bankRequestDto.setAmount(bd);
        bankRequestDto.setCurrency(currency);

        Result<TransactionDto> payresult = bankClient.handleDebit(bankRequestDto);
        if(!payresult.getMessage().equals("success")){
            return Result.error(400,payresult.getMessage());
        }
        return Result.success("Payment successfully.");
    }

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDTO orderCreateRequestDTO) {

        Order order = new Order();
        order.setUserId(orderCreateRequestDTO.getUserId());
        order.setStatus("PENDING_PAYMENT");
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        float totalAmount = 0f;

        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemDetailDto> itemDetails = new ArrayList<>();

        for(OrderItemRequestDTO itemReq : orderCreateRequestDTO.getItems()){
            ProductDto product = warehouseClient.getProductById(itemReq.getProductId());
            float subTotalAmount = product.getPrice() * itemReq.getQuantity();
            totalAmount += subTotalAmount;

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItems.add(orderItem);

            OrderItemDetailDto detailDTO = new OrderItemDetailDto(
                    product.getId(),
                    product.getName(),
                    itemReq.getQuantity(),
                    product.getPrice(),
                    subTotalAmount
            );
            itemDetails.add(detailDTO);
        }

        order.setTotalAmount(totalAmount);

        orderMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setTotalAmount(order.getTotalAmount());
        responseDto.setCreateTime(order.getCreateTime());
        responseDto.setItems(itemDetails);

        return responseDto;
    }


}

