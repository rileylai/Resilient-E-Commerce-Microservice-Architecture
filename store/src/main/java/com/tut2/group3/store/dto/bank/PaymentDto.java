package com.tut2.group3.store.dto.bank;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    @NotNull(message = "orderId is required")
    private long orderId;
    @NotNull(message = "customerId is required")
    private long customerId;
    @Positive(message = "amount must be positive")
    private double amount;
    private LocalDateTime createPaymentDate;

}
