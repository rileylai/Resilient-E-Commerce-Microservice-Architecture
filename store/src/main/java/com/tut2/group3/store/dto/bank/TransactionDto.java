package com.tut2.group3.store.dto.bank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;  // Transaction ID
    private String orderId;
    private String userId;
    private String txType;  // DEBIT or REFUND
    private BigDecimal amount;
    private String currency;
    private String status;  // SUCCESS, FAILED, PENDING
    private String bankTxId;  // Bank transaction ID
    private String message;
    private LocalDateTime createdAt;
}
