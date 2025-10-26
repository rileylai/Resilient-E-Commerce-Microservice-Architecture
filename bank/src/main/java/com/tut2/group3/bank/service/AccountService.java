package com.tut2.group3.bank.service;

import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.AccountRequestDTO;
import com.tut2.group3.bank.dto.AccountResponseDTO;

public interface AccountService {

    Result<AccountResponseDTO> createAccount(AccountRequestDTO dto);

    Result<AccountResponseDTO> getAccount(String userId, String currency);

    Result<AccountResponseDTO> updateAccount(String userId, String currency, AccountRequestDTO dto);

    Result<Void> deleteAccount(String userId, String currency);
}
