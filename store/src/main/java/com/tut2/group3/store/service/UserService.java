package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.user.LoginDto;
import com.tut2.group3.store.dto.user.RegisterDto;
import com.tut2.group3.store.dto.user.UserDto;
import com.tut2.group3.store.pojo.User;

public interface UserService {



    void register(RegisterDto registerDto);

    User login(LoginDto loginDto);

    UserDto findByUsername(String username);
}
