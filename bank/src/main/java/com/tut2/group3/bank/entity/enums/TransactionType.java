package com.tut2.group3.bank.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEBIT("DEBIT"),
    REFUND("REFUND");

    @EnumValue
    private final String value;

    TransactionType(String value) {
        this.value = value;
    }
}