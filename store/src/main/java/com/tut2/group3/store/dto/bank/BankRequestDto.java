package com.tut2.group3.store.dto.bank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankRequestDto {
    @NotBlank
    private String orderId;

    @NotBlank
    private String userId;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String currency;

    // Idempotency key for refund operations to prevent duplicate refunds
    private String idempotencyKey;

}
