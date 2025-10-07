package com.tut2.group3.bank.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum TransactionStatus {
    REQUESTED("REQUESTED"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED");

    @EnumValue
    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }
}