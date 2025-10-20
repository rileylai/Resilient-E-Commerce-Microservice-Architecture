package com.tut2.group3.store.dto.bank;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDto {
    private String orderId;
    private BigDecimal amount;
    private String status;
}
