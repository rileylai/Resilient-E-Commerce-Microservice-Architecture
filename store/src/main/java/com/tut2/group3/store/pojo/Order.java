package com.tut2.group3.store.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String status; // PENDING_VALIDATION, PENDING_PAYMENT, PAYMENT_SUCCESSFUL, DELIVERY_REQUESTED, COMPLETED, CANCELLED, FAILED
    private double totalAmount;
    private String reservationId; // Warehouse reservation ID
    private String transactionId; // Bank transaction ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
