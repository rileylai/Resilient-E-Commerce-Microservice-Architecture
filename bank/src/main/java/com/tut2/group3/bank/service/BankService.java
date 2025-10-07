package com.tut2.group3.bank.service;

import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.DebitRequestDTO;
import com.tut2.group3.bank.dto.RefundRequestDTO;
import com.tut2.group3.bank.entity.Transaction;

public interface BankService {

    Result<Transaction> processDebit(DebitRequestDTO dto);

    Result<Transaction> processRefund(RefundRequestDTO dto);
}
