package com.tut2.group3.bank.utils;

import com.tut2.group3.bank.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JWTUtil {

    private final JwtConfig jwtConfig;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        // build signing key once to avoid repeated derivation per request
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        // for debugging only !!!
        System.out.println("[DEBUG] JWT Secret = " + jwtConfig.getSecret());
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
