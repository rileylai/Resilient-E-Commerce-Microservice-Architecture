package com.tut2.group3.store.dto.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<OrderItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private Integer quantity;
    }
}

