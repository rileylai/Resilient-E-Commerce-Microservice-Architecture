package com.tut2.group3.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        log.info("Creating account for userId={} currency={}", dto.getUserId(), dto.getCurrency());

        if (existsByUserIdAndCurrency(dto.getUserId(), dto.getCurrency())) {
            return Result.error(ErrorCode.BAD_REQUEST,
                    String.format("Account already exists for user %s in currency %s",
                            dto.getUserId(), dto.getCurrency()));
        }

        Account account = modelMapper.map(dto, Account.class);
        account.setCreatedAt(LocalDateTime.now());

        accountMapper.insert(account);
        log.info("Account created for userId={} currency={}", dto.getUserId(), dto.getCurrency());

        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    public Result<AccountResponseDTO> getAccount(String userId, String currency) {
        Account account = findAccountByUserIdAndCurrencyOrThrow(userId, currency);
        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    @Transactional
    public Result<AccountResponseDTO> updateAccount(String userId, String currency, AccountRequestDTO dto) {
        log.info("Updating account for userId={} currency={}", userId, currency);

        Account account = findAccountByUserIdAndCurrencyOrThrow(userId, currency);

        account.setBalance(dto.getBalance());
        account.setCurrency(dto.getCurrency());
        accountMapper.updateById(account);

        return Result.success(modelMapper.map(account, AccountResponseDTO.class));
    }

    @Override
    @Transactional
    public Result<Void> deleteAccount(String userId, String currency) {
        log.info("Deleting account for userId={} currency={}", userId, currency);

        Account account = findAccountByUserIdAndCurrencyOrThrow(userId, currency);
        accountMapper.deleteById(account.getId());

        return Result.success();
    }

    private boolean existsByUserIdAndCurrency(String userId, String currency) {
        return accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getCurrency, currency)) > 0;
    }

    private Account findAccountByUserIdAndCurrencyOrThrow(String userId, String currency) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUserId, userId)
                .eq(Account::getCurrency, currency)
                .last("LIMIT 1"));

        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    String.format("Account not found for user %s and currency %s", userId, currency));
        }
        return account;
    }
}
