package com.tut2.group3.store.dto.bank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDto {

    private String userId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
}

