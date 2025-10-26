package com.tut2.group3.bank.controller;

import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.AccountRequestDTO;
import com.tut2.group3.bank.dto.AccountResponseDTO;
import com.tut2.group3.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public Result<AccountResponseDTO> createAccount(@Valid @RequestBody AccountRequestDTO dto) {
        log.info("REST call to create account for userId={} currency={}", dto.getUserId(), dto.getCurrency());
        return accountService.createAccount(dto);
    }

    @GetMapping("/{userId}/{currency}")
    public Result<AccountResponseDTO> getAccount(@PathVariable String userId,
                                                 @PathVariable String currency) {
        log.info("REST call to get account for userId={} currency={}", userId, currency);
        return accountService.getAccount(userId, currency);
    }

    @PutMapping("/{userId}/{currency}")
    public Result<AccountResponseDTO> updateAccount(@PathVariable String userId,
                                                    @PathVariable String currency,
                                                    @Valid @RequestBody AccountRequestDTO dto) {
        log.info("REST call to update account for userId={} currency={}", userId, currency);
        return accountService.updateAccount(userId, currency, dto);
    }

    @DeleteMapping("/{userId}/{currency}")
    public Result<Void> deleteAccount(@PathVariable String userId,
                                      @PathVariable String currency) {
        log.info("REST call to delete account for userId={} currency={}", userId, currency);
        return accountService.deleteAccount(userId, currency);
    }
}
