package com.tut2.group3.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.DebitRequestDTO;
import com.tut2.group3.bank.dto.RefundRequestDTO;
import com.tut2.group3.bank.entity.Account;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import com.tut2.group3.bank.repository.AccountRepository;
import com.tut2.group3.bank.repository.TransactionRepository;
import com.tut2.group3.bank.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Result<Transaction> processDebit(DebitRequestDTO dto) {
        log.info("Received debit request for orderId={}, userId={}, amount={} {}", dto.getOrderId(), dto.getUserId(), dto.getAmount(), dto.getCurrency());

        Account account = accountRepository.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, dto.getUserId())
                .last("limit 1"));

        Transaction transaction = createTransaction(dto, TransactionType.DEBIT, "TX");

        transactionRepository.insert(transaction);
        log.info("Persisted transaction id={} with bankTxId={}", transaction.getId(), transaction.getBankTxId());

        // Validate account
        if (account == null) {
            return failTransaction(transaction, "Account not found for user " + dto.getUserId(), ErrorCode.DEBIT_FAILED);
        }

        // Validate currency
        if (account.getCurrency() != null && dto.getCurrency() != null
                && !account.getCurrency().equalsIgnoreCase(dto.getCurrency())) {
            return failTransaction(transaction, "Currency mismatch for account " + account.getId(), ErrorCode.DEBIT_FAILED);
        }

        // Validate sufficient balance
        if (account.getBalance() == null || account.getBalance().compareTo(dto.getAmount()) < 0) {
            return failTransaction(transaction, "Insufficient funds", ErrorCode.DEBIT_FAILED);
        }

        // Simulates a random chance of success or failure
        // If the number is less than 0.5, we consider the transaction a success
        boolean success = ThreadLocalRandom.current().nextDouble() < 0.5;
        if (success) {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
            accountRepository.updateById(account);

            transaction.setStatus(TransactionStatus.SUCCEEDED);
            transaction.setMessage("Debit succeeded");
            transactionRepository.updateById(transaction);
            log.info("Transaction id={} succeeded; new balance={}", transaction.getId(), account.getBalance());
            return Result.success(transaction);
        }

        // Simulate failure
        return failTransaction(transaction, "Bank declined debit", ErrorCode.DEBIT_FAILED);
    }

    @Override
    @Transactional
    public Result<Transaction> processRefund(RefundRequestDTO dto) {
        log.info("Received refund request for orderId={}, amount={} {}", dto.getOrderId(), dto.getAmount(), dto.getCurrency());

        Transaction original = transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getOrderId, dto.getOrderId())
                .eq(Transaction::getTxType, TransactionType.DEBIT)
                .orderByDesc(Transaction::getCreatedAt)
                .last("limit 1"));

        // Validate original transaction
        if (original == null) {
            log.warn("Refund request rejected: no debit found for orderId={}", dto.getOrderId());
            return Result.error(ErrorCode.REFUND_FAILED, "Original debit not found for order " + dto.getOrderId());
        }

        // Only settled transactions can be refunded
        if (original.getStatus() != TransactionStatus.SUCCEEDED) {
            log.warn("Refund request rejected: original transaction id={} status={}", original.getId(), original.getStatus());
            return Result.error(ErrorCode.REFUND_FAILED, "Original transaction not settled for refund");
        }

        // Validate refund amount and currency
        if (dto.getAmount().compareTo(original.getAmount()) > 0) {
            log.warn("Refund request rejected: amount {} exceeds original {}", dto.getAmount(), original.getAmount());
            return Result.error(ErrorCode.REFUND_FAILED, "Refund amount exceeds original debit");
        }

        // Currency must match
        if (original.getCurrency() != null && dto.getCurrency() != null
                && !original.getCurrency().equalsIgnoreCase(dto.getCurrency())) {
            log.warn("Refund request rejected: currency mismatch for orderId={}", dto.getOrderId());
            return Result.error(ErrorCode.REFUND_FAILED, "Currency mismatch for refund");
        }

        Account account = accountRepository.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, original.getUserId())
                .last("limit 1"));

        // Validate account existence
        if (account == null) {
            log.warn("Refund request rejected: account not found for user {}", original.getUserId());
            return Result.error(ErrorCode.REFUND_FAILED, "Account not found for user " + original.getUserId());
        }

        Transaction refund = createTransaction(dto, TransactionType.REFUND, "RF");
        refund.setUserId(original.getUserId());

        transactionRepository.insert(refund);
        log.info("Persisted refund transaction id={} with bankTxId={}", refund.getId(), refund.getBankTxId());

        // Simulates a random chance of success or failure
        // If the number is less than 0.5, we consider the transaction a success
        boolean success = ThreadLocalRandom.current().nextDouble() < 0.5;
        if (success) {
            account.setBalance(account.getBalance().add(dto.getAmount()));
            accountRepository.updateById(account);

            refund.setStatus(TransactionStatus.SUCCEEDED);
            refund.setMessage("Refund succeeded");
            transactionRepository.updateById(refund);
            log.info("Refund transaction id={} succeeded; new balance={}", refund.getId(), account.getBalance());
            return Result.success(refund);
        }

        // Simulate failure
        return failTransaction(refund, "Bank declined refund", ErrorCode.REFUND_FAILED);
    }

    private Transaction createTransaction(Object source, TransactionType type, String prefix) {
        Transaction transaction = modelMapper.map(source, Transaction.class);
        transaction.setTxType(type);
        transaction.setStatus(TransactionStatus.REQUESTED);
        transaction.setBankTxId(generateTxId(prefix));
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    private Result<Transaction> failTransaction(Transaction transaction, String message, ErrorCode errorCode) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setMessage(message);
        transactionRepository.updateById(transaction);
        log.warn("Transaction id={} failed: {}", transaction.getId(), message);
        return Result.error(errorCode, message);
    }

    private String generateTxId(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}
