package com.tut2.group3.store.service.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tut2.group3.store.client.BankClient;
import com.tut2.group3.store.dto.bank.AccountRequestDto;
import com.tut2.group3.store.dto.bank.AccountResponseDto;
import com.tut2.group3.store.dto.user.LoginDto;
import com.tut2.group3.store.dto.user.RegisterDto;
import com.tut2.group3.store.dto.user.UserDto;
import com.tut2.group3.store.exception.BusinessException;
import com.tut2.group3.store.mapper.UserMapper;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.pojo.User;
import com.tut2.group3.store.service.UserService;
import com.tut2.group3.store.util.Md5Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BankClient bankClient;

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

        // Check if userId is 2 (reserved for Bank's receiving account)
        if (user.getId() == 2) {
            log.warn("User ID 2 is reserved for Bank's receiving account. Skipping this ID.");
            // Delete this user and create a new one to get a different ID
            this.removeById(user.getId());

            // Create user again to get next ID (3)
            User newUser = new User();
            newUser.setUsername(registerDto.getUsername());
            newUser.setEmail(registerDto.getEmail());
            newUser.setPassword(Md5Utils.md5(registerDto.getPassword()));
            this.save(newUser);
            user = newUser;
            log.info("User recreated with ID: {}", user.getId());
        }

        // Create corresponding bank account
        try {
            AccountRequestDto accountRequest = new AccountRequestDto();
            accountRequest.setUserId(String.valueOf(user.getId()));
            accountRequest.setBalance(BigDecimal.valueOf(1000.00)); // Initial balance: $1000
            accountRequest.setCurrency("AUD");

            Result<AccountResponseDto> bankResult = bankClient.createAccount(accountRequest);

            if (bankResult.getCode() == 200) {
                log.info("Bank account created successfully for user ID: {}, initial balance: $1000 AUD", user.getId());
            } else {
                log.error("Failed to create bank account for user ID: {}. Error: {}", user.getId(), bankResult.getMessage());
                // Note: We don't rollback user creation even if bank account creation fails
                // The user can still login, but may need manual intervention for bank account
            }
        } catch (Exception e) {
            log.error("Exception while creating bank account for user ID: {}. Error: {}", user.getId(), e.getMessage(), e);
            // Don't throw exception - allow registration to complete even if bank account creation fails
        }
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

    @Override
    public UserDto findByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery();
        queryWrapper.eq(User::getUsername, username);

        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);
        return userDto;
    }

}
