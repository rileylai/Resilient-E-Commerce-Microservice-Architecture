package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.user.LoginDto;
import com.tut2.group3.store.dto.user.RegisterDto;
import com.tut2.group3.store.pojo.Result;
import com.tut2.group3.store.pojo.User;
import com.tut2.group3.store.service.UserService;
import com.tut2.group3.store.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDto registerDto) {
        userService.register(registerDto);
        return Result.success("successful register");
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginDto loginDto) {
        User user = userService.login(loginDto);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());

        //generate token
        String token = jwtUtil.generateToken(claims);

        return Result.success(token);
    }

}
