package com.tut2.group3.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tut2.group3.store.pojo.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
