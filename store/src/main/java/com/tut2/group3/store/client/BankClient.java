package com.tut2.group3.store.client;

import com.tut2.group3.store.dto.bank.AccountRequestDto;
import com.tut2.group3.store.dto.bank.AccountResponseDto;
import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.TransactionDto;
import com.tut2.group3.store.pojo.Result;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "bank", url = "http://localhost:8084")
public interface BankClient {

    /**
     * Process debit transaction
     */
    @PostMapping("/api/bank/debit")
    Result<TransactionDto> handleDebit(@Valid @RequestBody BankRequestDto dto);

    /**
     * Process refund transaction
     */
    @PostMapping("/api/bank/refund")
    Result<TransactionDto> handleRefund(@Valid @RequestBody BankRequestDto dto);

    /**
     * Create a new bank account
     */
    @PostMapping("/api/account")
    Result<AccountResponseDto> createAccount(@Valid @RequestBody AccountRequestDto dto);

    /**
     * Get account by user ID
     */
    @GetMapping("/api/account/{userId}/{currency}")
    Result<AccountResponseDto> getAccount(@PathVariable("userId") String userId,
                                          @PathVariable("currency") String currency);

    /**
     * Update account information
     */
    @PutMapping("/api/account/{userId}/{currency}")
    Result<AccountResponseDto> updateAccount(@PathVariable("userId") String userId,
                                             @PathVariable("currency") String currency,
                                             @Valid @RequestBody AccountRequestDto dto);

    /**
     * Delete account by user ID
     */
    @DeleteMapping("/api/account/{userId}/{currency}")
    Result<Void> deleteAccount(@PathVariable("userId") String userId,
                               @PathVariable("currency") String currency);
}
