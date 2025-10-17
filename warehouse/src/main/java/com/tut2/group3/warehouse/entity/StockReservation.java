package com.tut2.group3.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("stock_reservation")
public class StockReservation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderId;

    private Long warehouseId;

    private Long productId;

    private Integer quantity;

    private String status; // RESERVED, CONFIRMED, RELEASED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
