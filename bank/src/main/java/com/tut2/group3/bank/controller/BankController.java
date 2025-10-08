package com.tut2.group3.bank.controller;

import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.dto.DebitRequestDTO;
import com.tut2.group3.bank.dto.RefundRequestDTO;
import com.tut2.group3.bank.entity.Transaction;
import com.tut2.group3.bank.service.BankService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bank")
public class BankController {

    private final BankService bankService;

    @PostMapping("/debit")
    public Result<Transaction> handleDebit(@Valid @RequestBody DebitRequestDTO dto,
                                           HttpServletRequest request) {
        String caller = (String) request.getAttribute("caller");
        log.info("Handling debit request for orderId={}, caller={}", dto.getOrderId(), caller);
        return bankService.processDebit(dto);
    }

    @PostMapping("/refund")
    public Result<Transaction> handleRefund(@Valid @RequestBody RefundRequestDTO dto,
                                            HttpServletRequest request) {
        String caller = (String) request.getAttribute("caller");
        log.info("Handling refund request for orderId={}, caller={}", dto.getOrderId(), caller);
        return bankService.processRefund(dto);
    }
}
