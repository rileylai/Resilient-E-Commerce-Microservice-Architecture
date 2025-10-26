package com.tut2.group3.bank.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionRequestEventDTO {

    private String eventType;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;

    public DebitRequestDTO toDebitRequest() {
        return new DebitRequestDTO(orderId, userId, amount, currency);
    }

    public RefundRequestDTO toRefundRequest() {
        return new RefundRequestDTO(orderId, userId, amount, currency, null);
    }
}
