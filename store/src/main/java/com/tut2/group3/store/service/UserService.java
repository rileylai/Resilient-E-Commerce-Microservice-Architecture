package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.LoginDto;
import com.tut2.group3.store.dto.RegisterDto;
import com.tut2.group3.store.pojo.User;

public interface UserService {

    void register(RegisterDto registerDto);

    User login(LoginDto loginDto);
}
