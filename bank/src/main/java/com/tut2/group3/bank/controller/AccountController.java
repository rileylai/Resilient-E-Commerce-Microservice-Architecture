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
        log.info("REST call to create account for userId={}", dto.getUserId());
        return accountService.createAccount(dto);
    }

    @GetMapping("/{userId}")
    public Result<AccountResponseDTO> getAccount(@PathVariable String userId) {
        log.info("REST call to get account for userId={}", userId);
        Result<AccountResponseDTO> result = accountService.getAccount(userId);
        if (result == null) {
            log.warn("AccountService returned null for userId={}", userId);
        } else {
            log.debug("AccountService responded with code={} for userId={}", result.getCode(), userId);
        }
        return result;
    }

    @PutMapping("/{userId}")
    public Result<AccountResponseDTO> updateAccount(@PathVariable String userId,
                                                    @Valid @RequestBody AccountRequestDTO dto) {
        log.info("REST call to update account for userId={}", userId);
        return accountService.updateAccount(userId, dto);
    }

    @DeleteMapping("/{userId}")
    public Result<Void> deleteAccount(@PathVariable String userId) {
        log.info("REST call to delete account for userId={}", userId);
        return accountService.deleteAccount(userId);
    }
}
