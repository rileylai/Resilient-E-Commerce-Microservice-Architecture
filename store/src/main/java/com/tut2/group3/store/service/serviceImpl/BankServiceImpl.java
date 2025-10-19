package com.tut2.group3.store.service.serviceImpl;

import com.tut2.group3.store.dto.bank.PaymentDto;
import com.tut2.group3.store.dto.bank.PaymentResponseDto;
import com.tut2.group3.store.service.BankService;
import org.springframework.stereotype.Service;

@Service
public class BankServiceImpl implements BankService {

    @Override
    public PaymentResponseDto Payment(PaymentDto paymentDto) {

        PaymentResponseDto paymentResponse = new PaymentResponseDto();
        return null;
    }
}
