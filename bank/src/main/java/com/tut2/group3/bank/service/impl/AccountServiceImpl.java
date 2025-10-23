package com.tut2.group3.bank.service.impl;

import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.AccountRequestDTO;
import com.tut2.group3.bank.dto.AccountResponseDTO;
import com.tut2.group3.bank.entity.Account;
import com.tut2.group3.bank.exception.BusinessException;
import com.tut2.group3.bank.mapper.AccountMapper;
import com.tut2.group3.bank.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public Result<AccountResponseDTO> createAccount(AccountRequestDTO dto) {
        log.info("Creating account for userId={}", dto.getUserId());

        if (existsByUserId(dto.getUserId())) {
            return Result.error(ErrorCode.BAD_REQUEST, "Account already exists for user " + dto.getUserId());
        }

        Account account = modelMapper.map(dto, Account.class);
        account.setCreatedAt(LocalDateTime.now());

        accountMapper.insertAccount(account);
        log.info("Account created for userId={}", dto.getUserId());

        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    public Result<AccountResponseDTO> getAccount(String userId) {
        Account account = findAccountByUserIdOrThrow(userId);
        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    @Transactional
    public Result<AccountResponseDTO> updateAccount(String userId, AccountRequestDTO dto) {
        log.info("Updating account for userId={}", userId);

        Account account = findAccountByUserIdOrThrow(userId);

        account.setBalance(dto.getBalance());
        account.setCurrency(dto.getCurrency());
        accountMapper.updateAccount(account);

        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    @Transactional
    public Result<Void> deleteAccount(String userId) {
        log.info("Deleting account for userId={}", userId);

        Account account = findAccountByUserIdOrThrow(userId);
        accountMapper.deleteById(account.getId());

        return Result.success();
    }

    private boolean existsByUserId(String userId) {
        return accountMapper.countByUserId(userId) > 0;
    }

    private Account findAccountByUserIdOrThrow(String userId) {
        Account account = accountMapper.selectByUserId(userId);

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Account not found for user " + userId);
        }
        return account;
    }
}
