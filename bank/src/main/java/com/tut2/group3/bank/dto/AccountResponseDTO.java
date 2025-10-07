package com.tut2.group3.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponseDTO {

    private String userId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
}
