package com.tut2.group3.bank.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.TransactionRequestEventDTO;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import com.tut2.group3.bank.producer.BankEventPublisher;
import com.tut2.group3.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionRequestHandlerService {

    private final BankService bankService;
    private final TransactionRepository transactionRepository;
    private final BankEventPublisher bankEventPublisher;

    public void handleTransactionRequest(TransactionRequestEventDTO request) {
        if (request == null) {
            log.warn("Received null transaction request message");
            return;
        }

        String normalizedEventType = normalizeEventType(request.getEventType());
        TransactionType txType = resolveTransactionType(normalizedEventType);
        if (txType == null) {
            log.warn("Unsupported transaction request eventType={} orderId={}", request.getEventType(), request.getOrderId());
            return;
        }

        Transaction existing = findLatestTransaction(request.getOrderId(), txType);
        if (isIdempotentHit(existing)) {
            log.info("Skipping duplicate transaction orderId={} txType={} status={} idempotencyKey={}",
                    request.getOrderId(), txType, existing.getStatus(), request.getIdempotencyKey());
            bankEventPublisher.publishTransactionResult(existing, true);
            return;
        }

        Result<Transaction> result = dispatchToService(request, txType);
        Transaction transaction = result != null ? result.getData() : null;
        boolean processedByService = result != null && result.getData() != null;

        if (transaction == null) {
            transaction = findLatestTransaction(request.getOrderId(), txType);
        }

        if (transaction != null && transaction.getStatus() != TransactionStatus.REQUESTED) {
            if (!processedByService) {
                // Publish transaction result event to RabbitMQ
                bankEventPublisher.publishTransactionResult(transaction);
            } else {
                log.debug("Result already published by service for orderId={} txId={}", transaction.getOrderId(), transaction.getId());
            }
        } else {
            log.warn("Unable to publish transaction result for orderId={} eventType={} status={}",
                    request.getOrderId(), request.getEventType(),
                    transaction != null ? transaction.getStatus() : null);
        }
    }

    private Result<Transaction> dispatchToService(TransactionRequestEventDTO request, TransactionType txType) {
        switch (txType) {
            case DEBIT -> {
                return bankService.processDebit(request.toDebitRequest());
            }
            case REFUND -> {
                return bankService.processRefund(request.toRefundRequest());
            }
            default -> {
                return Result.success();
            }
        }
    }

    private Transaction findLatestTransaction(String orderId, TransactionType txType) {
        if (!StringUtils.hasText(orderId) || txType == null) {
            return null;
        }

        return transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getOrderId, orderId)
                .eq(Transaction::getTxType, txType)
                .orderByDesc(Transaction::getCreatedAt)
                .last("limit 1"));
    }

    private boolean isIdempotentHit(Transaction transaction) {
        return transaction != null && transaction.getStatus() == TransactionStatus.SUCCEEDED;
    }

    private TransactionType resolveTransactionType(String normalizedEventType) {
        if (normalizedEventType == null) {
            return null;
        }
        return switch (normalizedEventType) {
            case "DEBITREQUEST", "DEBIT", "DEBIT_REQUEST" -> TransactionType.DEBIT;
            case "REFUNDREQUEST", "REFUND", "REFUND_REQUEST" -> TransactionType.REFUND;
            default -> null;
        };
    }

    private String normalizeEventType(String rawEventType) {
        return rawEventType == null ? null : rawEventType.trim().toUpperCase();
    }
}
