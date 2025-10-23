package com.tut2.group3.bank.repository;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.bank.entity.Transaction;

@Mapper
public interface TransactionRepository extends BaseMapper<Transaction> {
}

