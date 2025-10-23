package com.tut2.group3.warehouse.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warehouseId;

    private Long productId;

    private Integer availableQuantity;

    private Integer reservedQuantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Version
    private Integer version; // For optimistic locking
}
