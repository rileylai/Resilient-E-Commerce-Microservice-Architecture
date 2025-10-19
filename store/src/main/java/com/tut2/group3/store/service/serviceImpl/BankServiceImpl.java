package com.tut2.group3.store.service.serviceImpl;

import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.BankResponseDto;
import com.tut2.group3.store.service.BankService;
import org.springframework.stereotype.Service;

@Service
public class BankServiceImpl implements BankService {

    @Override
    public BankResponseDto Payment(BankRequestDto bankRequestDto) {

        BankResponseDto bankResponseDto = new BankResponseDto();

        // send to bank service and get response

        return bankResponseDto;
    }
}
