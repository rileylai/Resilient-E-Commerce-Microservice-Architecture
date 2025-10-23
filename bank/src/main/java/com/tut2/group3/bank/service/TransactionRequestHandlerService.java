package com.tut2.group3.bank.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.TransactionRequestEventDTO;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.entity.enums.TransactionStatus;
import com.tut2.group3.bank.entity.enums.TransactionType;
import com.tut2.group3.bank.producer.BankEventPublisher;
import com.tut2.group3.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


public interface TransactionRequestHandlerService {

    void handleTransactionRequest(TransactionRequestEventDTO request);

}
