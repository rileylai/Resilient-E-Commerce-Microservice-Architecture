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
import com.tut2.group3.bank.mapper.AccountMapper;
import com.tut2.group3.bank.producer.BankEventPublisher;
import com.tut2.group3.bank.repository.TransactionRepository;
import com.tut2.group3.bank.service.BankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {

    private static final String STORE_USER_ID = "2";

    private final TransactionRepository transactionRepository;
    private final AccountMapper accountMapper;
    private final ModelMapper modelMapper;
    private final BankEventPublisher bankEventPublisher;

    @Override
    @Transactional
    public Result<Transaction> processDebit(DebitRequestDTO dto) {
        log.info("Received debit request for orderId={}, userId={}, amount={} {}", dto.getOrderId(), dto.getUserId(), dto.getAmount(), dto.getCurrency());

        Transaction existingDebit = transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getOrderId, dto.getOrderId())
                .eq(Transaction::getTxType, TransactionType.DEBIT)
                .eq(Transaction::getStatus, TransactionStatus.SUCCEEDED));
        if (existingDebit != null) {
            log.warn("Duplicate debit detected for orderId={}", dto.getOrderId());
            return Result.error(ErrorCode.DEBIT_FAILED, "Order already debited");
        }

        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, dto.getUserId())
                .eq(Account::getCurrency, dto.getCurrency())
                .last("LIMIT 1 FOR UPDATE"));

        Account storeAccount = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, STORE_USER_ID)
                .eq(Account::getCurrency, dto.getCurrency())
                .last("LIMIT 1 FOR UPDATE"));

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

        // Validate store account (id=2)
        if (storeAccount == null) {
            return failTransaction(transaction, "Store account not found for currency " + dto.getCurrency(), ErrorCode.DEBIT_FAILED);
        }

        // Validate sufficient balance
        if (account.getBalance() == null || account.getBalance().compareTo(dto.getAmount()) < 0) {
            return failTransaction(transaction, "Insufficient funds", ErrorCode.DEBIT_FAILED);
        }

        // Simulates a random chance of success or failure
        // If the number is less than 0.5, we consider the transaction a success (now set to 1, all succeed)
        boolean success = ThreadLocalRandom.current().nextDouble() < 1;
        if (success) {
            account.setBalance(account.getBalance().subtract(dto.getAmount()));
            accountMapper.updateById(account);

            BigDecimal storeCurrentBalance = safe(storeAccount.getBalance());
            storeAccount.setBalance(storeCurrentBalance.add(dto.getAmount()));
            accountMapper.updateById(storeAccount);

            transaction.setStatus(TransactionStatus.SUCCEEDED);
            transaction.setMessage("Debit succeeded");
            transactionRepository.updateById(transaction);
            log.info("Transaction id={} succeeded; new balance={}", transaction.getId(), account.getBalance());
            bankEventPublisher.publishTransactionResult(transaction);
            return Result.success(transaction);
        }

        // Simulate failure
        return failTransaction(transaction, "Bank declined debit", ErrorCode.DEBIT_FAILED);
    }

    @Override
    @Transactional
    public Result<Transaction> processRefund(RefundRequestDTO dto) {
        log.info("Received refund request for orderId={}, userId={}, amount={} {}, idempotencyKey={}",
                dto.getOrderId(), dto.getUserId(), dto.getAmount(), dto.getCurrency(), dto.getIdempotencyKey());

        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Refund request rejected: non-positive amount orderId={} amount={}", dto.getOrderId(), dto.getAmount());
            return refundFailure(dto, dto.getUserId(), "Refund amount must be greater than zero");
        }

        Transaction existingByKey = null;
        if (StringUtils.hasText(dto.getIdempotencyKey())) {
            existingByKey = transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getIdempotencyKey, dto.getIdempotencyKey()));
            if (existingByKey != null) {
                log.info("Refund idempotency hit for key={} status={}", dto.getIdempotencyKey(), existingByKey.getStatus());
                bankEventPublisher.publishTransactionResult(existingByKey, true);
                if (existingByKey.getStatus() == TransactionStatus.SUCCEEDED) {
                    return Result.success(existingByKey);
                }
                return Result.error(ErrorCode.REFUND_FAILED,
                        existingByKey.getMessage() != null ? existingByKey.getMessage() : ErrorCode.REFUND_FAILED.getMessage());
            }
        }

        List<Transaction> orderTransactions = transactionRepository.selectList(new LambdaQueryWrapper<Transaction>()
                .eq(Transaction::getOrderId, dto.getOrderId())
                .last("FOR UPDATE"));

        if (CollectionUtils.isEmpty(orderTransactions)) {
            log.warn("Refund request rejected: no transactions found for orderId={}", dto.getOrderId());
            return refundFailure(dto, dto.getUserId(), "Original debit not found for order " + dto.getOrderId());
        }

        Transaction originalDebit = orderTransactions.stream()
                .filter(tx -> tx.getTxType() == TransactionType.DEBIT && tx.getStatus() == TransactionStatus.SUCCEEDED)
                .max(Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (originalDebit == null) {
            log.warn("Refund request rejected: no settled debit found for orderId={}", dto.getOrderId());
            return refundFailure(dto, dto.getUserId(), "Original transaction not settled for refund");
        }

        if (originalDebit.getCurrency() != null && dto.getCurrency() != null
                && !originalDebit.getCurrency().equalsIgnoreCase(dto.getCurrency())) {
            log.warn("Refund request rejected: currency mismatch for orderId={} original={} refund={}",
                    dto.getOrderId(), originalDebit.getCurrency(), dto.getCurrency());
            return refundFailure(dto, originalDebit.getUserId(), "Currency mismatch for refund");
        }

        BigDecimal debitAmount = safe(originalDebit.getAmount());
        BigDecimal refundedAmount = orderTransactions.stream()
                .filter(tx -> tx.getTxType() == TransactionType.REFUND && tx.getStatus() == TransactionStatus.SUCCEEDED)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedRefundTotal = refundedAmount.add(dto.getAmount());
        if (projectedRefundTotal.compareTo(debitAmount) > 0) {
            log.warn("Refund request rejected: refund total would exceed debit orderId={} debitAmount={} refunded={} requested={}",
                    dto.getOrderId(), debitAmount, refundedAmount, dto.getAmount());
            return refundFailure(dto, originalDebit.getUserId(), "Refund exceeds available balance");
        }

        Account customerAccount = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, originalDebit.getUserId())
                .eq(Account::getCurrency, originalDebit.getCurrency())
                .last("LIMIT 1 FOR UPDATE"));

        if (customerAccount == null) {
            log.warn("Refund request rejected: account not found for user {}", originalDebit.getUserId());
            return refundFailure(dto, originalDebit.getUserId(), "Account not found for user " + originalDebit.getUserId());
        }

        if (customerAccount.getCurrency() != null && dto.getCurrency() != null
                && !customerAccount.getCurrency().equalsIgnoreCase(dto.getCurrency())) {
            log.warn("Refund request rejected: account currency mismatch userId={} accountCurrency={} refundCurrency={}",
                    customerAccount.getUserId(), customerAccount.getCurrency(), dto.getCurrency());
            return refundFailure(dto, customerAccount.getUserId(), "Account currency mismatch for refund");
        }

        Account storeAccount = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, STORE_USER_ID)
                .eq(Account::getCurrency, originalDebit.getCurrency())
                .last("LIMIT 1 FOR UPDATE"));

        if (storeAccount == null) {
            log.warn("Refund request rejected: store account not found for currency {}", dto.getCurrency());
            return refundFailure(dto, originalDebit.getUserId(), "Store account not found for currency " + dto.getCurrency());
        }

        BigDecimal storeAvailable = safe(storeAccount.getBalance());
        if (storeAvailable.compareTo(dto.getAmount()) < 0) {
            log.warn("Refund request rejected: store account insufficient funds currency={} balance={} requested={}",
                    storeAccount.getCurrency(), storeAccount.getBalance(), dto.getAmount());
            return refundFailure(dto, originalDebit.getUserId(), "Store account insufficient funds");
        }

        if (StringUtils.hasText(dto.getIdempotencyKey())) {
            Transaction duplicateAfterLock = transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getIdempotencyKey, dto.getIdempotencyKey())
                    .last("FOR UPDATE"));
            if (duplicateAfterLock != null) {
                log.info("Refund idempotency key={} already processed after lock with status={}",
                        dto.getIdempotencyKey(), duplicateAfterLock.getStatus());
                bankEventPublisher.publishTransactionResult(duplicateAfterLock, true);
                if (duplicateAfterLock.getStatus() == TransactionStatus.SUCCEEDED) {
                    return Result.success(duplicateAfterLock);
                }
                return Result.error(ErrorCode.REFUND_FAILED,
                        duplicateAfterLock.getMessage() != null ? duplicateAfterLock.getMessage() : ErrorCode.REFUND_FAILED.getMessage());
            }
        }

        Transaction refund = createTransaction(dto, TransactionType.REFUND, "RF");
        refund.setUserId(originalDebit.getUserId());
        refund.setIdempotencyKey(dto.getIdempotencyKey());
        refund.setStatus(TransactionStatus.SUCCEEDED);
        refund.setMessage("Refund succeeded");

        try {
            transactionRepository.insert(refund);
            log.info("Persisted refund transaction id={} bankTxId={} orderId={} amount={}",
                    refund.getId(), refund.getBankTxId(), refund.getOrderId(), refund.getAmount());
        } catch (DuplicateKeyException duplicateKeyException) {
            log.info("Refund insert skipped due to duplicate idempotency key={} orderId={}",
                    dto.getIdempotencyKey(), dto.getOrderId());
            Transaction persisted = transactionRepository.selectOne(new LambdaQueryWrapper<Transaction>()
                    .eq(Transaction::getIdempotencyKey, dto.getIdempotencyKey()));
            if (persisted != null) {
                bankEventPublisher.publishTransactionResult(persisted, true);
                return Result.success(persisted);
            }
            throw duplicateKeyException;
        }

        BigDecimal clientCurrentBalance = safe(customerAccount.getBalance());
        customerAccount.setBalance(clientCurrentBalance.add(dto.getAmount()));
        accountMapper.updateById(customerAccount);

        storeAccount.setBalance(storeAvailable.subtract(dto.getAmount()));
        accountMapper.updateById(storeAccount);

        log.info("Refund transaction id={} succeeded; cumulative refunded={} of debit={} for orderId={}",
                refund.getId(), projectedRefundTotal, debitAmount, dto.getOrderId());
        bankEventPublisher.publishTransactionResult(refund);
        return Result.success(refund);
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
        if (transaction.getId() != null) {
            transactionRepository.updateById(transaction);
        }
        log.warn("Transaction id={} failed: {}", transaction.getId(), message);
        bankEventPublisher.publishTransactionResult(transaction);
        return Result.error(errorCode, message);
    }

    private String generateTxId(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    private Result<Transaction> refundFailure(RefundRequestDTO dto, String userId, String message) {
        Transaction failure = new Transaction();
        failure.setOrderId(dto.getOrderId());
        failure.setUserId(userId);
        failure.setTxType(TransactionType.REFUND);
        failure.setAmount(dto.getAmount());
        failure.setCurrency(dto.getCurrency());
        failure.setStatus(TransactionStatus.FAILED);
        failure.setMessage(message);
        failure.setBankTxId(generateTxId("RF"));
        failure.setCreatedAt(LocalDateTime.now());
        failure.setIdempotencyKey(dto.getIdempotencyKey());
        bankEventPublisher.publishTransactionResult(failure, true);
        return Result.error(ErrorCode.REFUND_FAILED, message);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
