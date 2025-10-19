package com.tut2.group3.store.dto.bank;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankResponseDto {
    @NotNull(message = "orderId is required")
    private Long orderId;
    @NotNull(message = "customerId is required")
    private Long customerId;
    private boolean ifSuccess;
    private String message;
}
