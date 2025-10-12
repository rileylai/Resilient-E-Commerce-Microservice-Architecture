package com.tut2.group3.store.service.serviceImpl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tut2.group3.store.dto.LoginDto;
import com.tut2.group3.store.dto.RegisterDto;
import com.tut2.group3.store.exception.BusinessException;
import com.tut2.group3.store.mapper.UserMapper;
import com.tut2.group3.store.pojo.User;
import com.tut2.group3.store.service.UserService;
import com.tut2.group3.store.util.Md5Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void register(RegisterDto registerDto) {

        // input check
        if (registerDto == null) {
            throw new BusinessException(400, "Register info is empty");
        }

        // only one check
        if (lambdaQuery().eq(User::getUsername, registerDto.getUsername()).exists()) {
            throw new BusinessException(409, "Username is already exist");
        }
        if (lambdaQuery().eq(User::getEmail, registerDto.getEmail()).exists()) {
            throw new BusinessException(409, "Email is already exist");
        }

        // Change Dto to Entity and save
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(Md5Utils.md5(registerDto.getPassword()));

        this.save(user);
    }

    @Override
    public User login(LoginDto loginDto) {
        // input check
        if (loginDto == null) {
            throw new BusinessException(400, "Login info is empty");
        }

        User user = lambdaQuery()
                .eq(User::getUsername, loginDto.getUsername())
                .one();

        if (user == null) {
            throw new BusinessException(404, "User not found");
        }

        // password equal
        String encryptedInputPassword = Md5Utils.md5(loginDto.getPassword());
        if (!encryptedInputPassword.equals(user.getPassword())) {
            throw new BusinessException(401, "Invalid password");
        }
        return user;
    }

}
