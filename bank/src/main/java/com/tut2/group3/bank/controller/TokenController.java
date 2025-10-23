// src/main/java/com/demo/api/controller/TokenController.java
package com.tut2.group3.bank.controller;

import com.tut2.group3.bank.service.impl.JwtServiceImpl;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    private final JwtServiceImpl jwtService;

    public TokenController(JwtServiceImpl jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/generate")
    public String generateToken(
            @RequestParam Long userId,
            @RequestParam String role
    ) {
        return jwtService.generateToken(userId, role);
    }
}