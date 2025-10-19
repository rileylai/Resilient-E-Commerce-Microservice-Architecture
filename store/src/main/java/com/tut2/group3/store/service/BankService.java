package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.bank.PaymentDto;
import com.tut2.group3.store.dto.bank.PaymentResponseDto;

public interface BankService {

    PaymentResponseDto Payment(PaymentDto paymentDto);

}
