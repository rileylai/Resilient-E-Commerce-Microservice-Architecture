// src/main/java/com/demo/api/service/JwtService.java
package com.tut2.group3.bank.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tut2.group3.bank.service.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))     // userId -> sub
                .claim("role", role)                    // 自定義 claim
                .setIssuedAt(new Date())                // 發行時間
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))  // 使用 secret 加簽
                .compact();
    }
}