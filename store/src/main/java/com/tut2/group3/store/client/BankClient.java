package com.tut2.group3.store.client;

import com.tut2.group3.store.dto.bank.BankRequestDto;
import com.tut2.group3.store.dto.bank.TransactionDto;
import com.tut2.group3.store.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "bank", url = "http://localhost:8082")
public interface BankClient {

    @PostMapping("/api/bank/debit")
    Result<TransactionDto> handleDebit(@Valid @RequestBody BankRequestDto dto);

}
