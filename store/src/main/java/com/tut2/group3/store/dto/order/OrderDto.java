package com.tut2.group3.store.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    private Long id;
    @NotBlank(message = "User cannot be empty")
    private Long userId;
    private String status;
    private Double totalAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<OrderItemDto> items;

}
