package com.tut2.group3.store.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseStockRequest {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Reservation ID is required")
    private String reservationId;

    private String reason; // ORDER_CANCELLED, PAYMENT_FAILED, etc.
}

