package com.tut2.group3.bank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResultEventDTO {

    private String eventType;
    private Long transactionId;
    private String bankTxId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime eventTime;

    public static TransactionResultEventDTO fromTransaction(Transaction transaction, String eventType) {
        TransactionType txType = transaction.getTxType();
        TransactionStatus status = transaction.getStatus();
        return TransactionResultEventDTO.builder()
                .eventType(eventType)
                .transactionId(transaction.getId())
                .bankTxId(transaction.getBankTxId())
                .orderId(transaction.getOrderId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .transactionType(txType != null ? txType.name() : null)
                .status(status != null ? status.name() : null)
                .message(transaction.getMessage())
                .createdAt(transaction.getCreatedAt())
                .eventTime(LocalDateTime.now())
                .build();
    }
}
