package com.tut2.group3.bank.mapper;

import com.tut2.group3.bank.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    Account selectById(@Param("id") Long id);

    Account selectByUserId(@Param("userId") String userId);

    int insertAccount(Account account);

    int updateAccount(Account account);

    int deleteById(@Param("id") Long id);

    int countByUserId(@Param("userId") String userId);
}
