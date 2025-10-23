package com.tut2.group3.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.bank.entity.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
