package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.BankResponseDto;

public interface BankService {

    BankResponseDto Payment(BankRequestDto bankRequestDto);

}
