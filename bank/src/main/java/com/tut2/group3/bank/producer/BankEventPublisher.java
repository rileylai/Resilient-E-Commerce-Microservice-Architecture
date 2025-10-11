package com.tut2.group3.bank.producer;

import com.tut2.group3.bank.config.BankRabbitProperties;
import com.tut2.group3.bank.dto.TransactionResultEventDTO;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final BankRabbitProperties rabbitProperties;
    private final ConcurrentMap<String, TransactionStatus> publishedTransactions = new ConcurrentHashMap<>();

    public void publishTransactionResult(Transaction transaction) {
        publishTransactionResult(transaction, false);
    }

    public void publishTransactionResult(Transaction transaction, boolean forcePublish) {
        if (transaction == null) {
            log.warn("Attempted to publish transaction event with null transaction");
            return;
        }

        TransactionType txType = transaction.getTxType();
        TransactionStatus status = transaction.getStatus();
        if (!forcePublish && isDuplicatePublication(transaction, status)) {
            log.debug("Skipping duplicate event for bankTxId={} status={}", transaction.getBankTxId(), status);
            return;
        }

        String eventType = resolveEventType(txType, status);
        TransactionResultEventDTO payload = TransactionResultEventDTO.fromTransaction(transaction, eventType);

        try {
            // Publish transaction result event to RabbitMQ
            rabbitTemplate.convertAndSend(
                    rabbitProperties.resolvedExchangeName(),
                    rabbitProperties.resolvedRoutingKey(),
                    payload
            );
            log.info("Published transaction event eventType={} orderId={} transactionId={} status={}",
                    eventType, transaction.getOrderId(), transaction.getId(), status);
        } catch (AmqpException ex) {
            log.error("Failed to publish transaction event eventType={} orderId={} transactionId={} due to {}",
                    eventType, transaction.getOrderId(), transaction.getId(), ex.getMessage(), ex);
        }
    }

    private boolean isDuplicatePublication(Transaction transaction, TransactionStatus status) {
        String txKey = transaction.getBankTxId();
        if (txKey == null || txKey.isBlank()) {
            txKey = transaction.getTxType() + ":" + transaction.getOrderId();
        }
        TransactionStatus previousStatus = publishedTransactions.putIfAbsent(txKey, status);
        return previousStatus != null && previousStatus == status;
    }

    private String resolveEventType(TransactionType txType, TransactionStatus status) {
        String operation = txType == TransactionType.REFUND ? "Refund" : "Debit";
        if (status == TransactionStatus.SUCCEEDED) {
            return operation + "Succeeded";
        }
        if (status == TransactionStatus.FAILED) {
            return operation + "Failed";
        }
        return operation + "Updated";
    }
}
