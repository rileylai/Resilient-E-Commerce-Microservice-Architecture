package com.tut2.group3.store.entity;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Order {
    private String id;
    private String orderNo;
    @NotEmpty
    private LocalDateTime createTime;
    @NotEmpty
    private LocalDateTime updateTime;
    @NotEmpty
    private String status;
    @NotEmpty
    private Integer userId;
    @NotEmpty
    private Integer productId;
    @NotEmpty
    private Integer quantity;
    @NotEmpty
    private float totalAmount;

}
