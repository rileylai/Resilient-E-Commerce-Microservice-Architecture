package com.tut2.group3.bank.service.impl;

import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.config.ModelMapperConfig;
import com.tut2.group3.bank.dto.RefundRequestDTO;
import com.tut2.group3.bank.entity.Account;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import com.tut2.group3.bank.mapper.AccountMapper;
import com.tut2.group3.bank.producer.BankEventPublisher;
import com.tut2.group3.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private BankEventPublisher bankEventPublisher;

    private BankServiceImpl bankService;

    @BeforeEach
    void setUp() {
        ModelMapper mapper = new ModelMapperConfig().modelMapper();
        bankService = new BankServiceImpl(transactionRepository, accountMapper, mapper, bankEventPublisher);
    }

    @Test
    void testRefund_success_if_under_limit() {
        RefundRequestDTO request = refundRequest("order-123", "user-1", "AUD", "key-success", new BigDecimal("20.00"));

        Transaction debit = debitTransaction("order-123", "user-1", "AUD", new BigDecimal("100.00"));
        Transaction priorRefund = refundTransaction("order-123", "user-1", "AUD", new BigDecimal("30.00"));
        Account account = account("user-1", "AUD", new BigDecimal("50.00"));

        when(transactionRepository.selectOne(any())).thenReturn(null);
        when(transactionRepository.selectList(any())).thenReturn(List.of(debit, priorRefund));
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);

        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(200L);
            return 1;
        }).when(transactionRepository).insert(any(Transaction.class));

        Result<Transaction> result = bankService.processRefund(request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertNotNull(result.getData());
        assertEquals(TransactionStatus.SUCCEEDED, result.getData().getStatus());
        assertEquals(0, account.getBalance().compareTo(new BigDecimal("70.00")));

        verify(transactionRepository).insert(any(Transaction.class));
        verify(accountMapper).updateById(any(Account.class));
        verify(bankEventPublisher).publishTransactionResult(any(Transaction.class));
    }

    @Test
    void testRefund_fails_if_exceeds_limit() {
        RefundRequestDTO request = refundRequest("order-456", "user-9", "AUD", "key-fail", new BigDecimal("25.00"));

        Transaction debit = debitTransaction("order-456", "user-9", "AUD", new BigDecimal("100.00"));
        Transaction priorRefund = refundTransaction("order-456", "user-9", "AUD", new BigDecimal("90.00"));

        when(transactionRepository.selectOne(any())).thenReturn(null);
        when(transactionRepository.selectList(any())).thenReturn(List.of(debit, priorRefund));

        Result<Transaction> result = bankService.processRefund(request);

        assertEquals(ErrorCode.REFUND_FAILED.getCode(), result.getCode());
        verify(transactionRepository, times(0)).insert(any(Transaction.class));

        ArgumentCaptor<Transaction> eventCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(bankEventPublisher).publishTransactionResult(eventCaptor.capture(), anyBoolean());
        Transaction published = eventCaptor.getValue();
        assertNotNull(published);
        assertEquals(TransactionStatus.FAILED, published.getStatus());
        assertEquals("Refund exceeds available balance", published.getMessage());
    }

    @Test
    void testRefund_idempotent_for_same_key() {
        RefundRequestDTO request = refundRequest("order-789", "user-3", "AUD", "key-idem", new BigDecimal("40.00"));

        Transaction debit = debitTransaction("order-789", "user-3", "AUD", new BigDecimal("100.00"));
        Account account = account("user-3", "AUD", new BigDecimal("60.00"));
        AtomicReference<Transaction> stored = new AtomicReference<>();

        when(transactionRepository.selectOne(any())).thenAnswer(invocation -> stored.get());
        when(transactionRepository.selectList(any())).thenReturn(List.of(debit));
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);

        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(300L);
            stored.set(cloneTransaction(tx));
            return 1;
        }).when(transactionRepository).insert(any(Transaction.class));

        Result<Transaction> firstResult = bankService.processRefund(request);
        Result<Transaction> secondResult = bankService.processRefund(request);

        assertEquals(ErrorCode.SUCCESS.getCode(), firstResult.getCode());
        assertEquals(ErrorCode.SUCCESS.getCode(), secondResult.getCode());
        assertNotNull(firstResult.getData());
        assertNotNull(secondResult.getData());
        assertEquals(firstResult.getData().getId(), secondResult.getData().getId());

        verify(transactionRepository).insert(any(Transaction.class));
        verify(accountMapper).updateById(any(Account.class));
        verify(bankEventPublisher, times(1)).publishTransactionResult(any(Transaction.class));
        verify(bankEventPublisher, times(1)).publishTransactionResult(any(Transaction.class), anyBoolean());
    }

    @Test
    void testRefund_concurrent_requests_only_one_succeeds() throws Exception {
        Transaction debit = debitTransaction("order-con", "user-5", "AUD", new BigDecimal("150.00"));
        AtomicReference<Transaction> stored = new AtomicReference<>();
        AtomicBoolean firstInsert = new AtomicBoolean(true);

        when(transactionRepository.selectOne(any())).thenAnswer(invocation -> stored.get());
        when(transactionRepository.selectList(any())).thenAnswer(invocation -> new ArrayList<>(List.of(debit)));
        when(accountMapper.selectOne(any())).thenAnswer(invocation -> account("user-5", "AUD", new BigDecimal("80.00")));
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);

        doAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            if (firstInsert.compareAndSet(true, false)) {
                tx.setId(400L);
                stored.set(cloneTransaction(tx));
                return 1;
            }
            throw new DuplicateKeyException("duplicate idempotency");
        }).when(transactionRepository).insert(any(Transaction.class));

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            var future1 = executorService.submit(() -> bankService.processRefund(
                    refundRequest("order-con", "user-5", "AUD", "key-concurrent", new BigDecimal("20.00"))));
            var future2 = executorService.submit(() -> bankService.processRefund(
                    refundRequest("order-con", "user-5", "AUD", "key-concurrent", new BigDecimal("20.00"))));

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);

            List<Result<Transaction>> results = new ArrayList<>();
            results.add(future1.get());
            results.add(future2.get());

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(result -> result.getCode() == ErrorCode.SUCCESS.getCode()));
            Optional<Transaction> persisted = results.stream()
                    .map(Result::getData)
                    .filter(Objects::nonNull)
                    .findFirst();
            assertTrue(persisted.isPresent());
        } finally {
            executorService.shutdown();
        }

        verify(accountMapper, times(1)).updateById(any(Account.class));
        verify(bankEventPublisher, times(1)).publishTransactionResult(any(Transaction.class));
        verify(bankEventPublisher, times(1)).publishTransactionResult(any(Transaction.class), anyBoolean());
    }

    private RefundRequestDTO refundRequest(String orderId, String userId, String currency, String key, BigDecimal amount) {
        return new RefundRequestDTO(orderId, userId, amount, currency, key);
    }

    private Transaction debitTransaction(String orderId, String userId, String currency, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setOrderId(orderId);
        transaction.setUserId(userId);
        transaction.setTxType(TransactionType.DEBIT);
        transaction.setStatus(TransactionStatus.SUCCEEDED);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        return transaction;
    }

    private Transaction refundTransaction(String orderId, String userId, String currency, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setId(20L);
        transaction.setOrderId(orderId);
        transaction.setUserId(userId);
        transaction.setTxType(TransactionType.REFUND);
        transaction.setStatus(TransactionStatus.SUCCEEDED);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        return transaction;
    }

    private Account account(String userId, String currency, BigDecimal balance) {
        Account account = new Account();
        account.setId(100L);
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setBalance(balance);
        return account;
    }

    private Transaction cloneTransaction(Transaction source) {
        Transaction clone = new Transaction();
        clone.setId(source.getId());
        clone.setOrderId(source.getOrderId());
        clone.setUserId(source.getUserId());
        clone.setTxType(source.getTxType());
        clone.setAmount(source.getAmount());
        clone.setCurrency(source.getCurrency());
        clone.setStatus(source.getStatus());
        clone.setMessage(source.getMessage());
        clone.setBankTxId(source.getBankTxId());
        clone.setCreatedAt(source.getCreatedAt());
        clone.setIdempotencyKey(source.getIdempotencyKey());
        return clone;
    }
}
