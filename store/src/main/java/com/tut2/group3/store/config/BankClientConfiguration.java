package com.tut2.group3.store.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * Feign Client Configuration for Bank Service
 * 
 * Adds JWT token authentication to all requests sent to Bank service.
 * The token includes role="store" which is required by Bank's security configuration.
 */
@Slf4j
@Configuration
public class BankClientConfiguration {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Create a RequestInterceptor that adds JWT token to all Bank service requests
     */
    @Bean
    public RequestInterceptor bankAuthInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Generate JWT token for Store service to authenticate with Bank
                String token = generateBankToken();
                template.header("Authorization", "Bearer " + token);
                log.debug("Added JWT token to Bank service request: {} {}", 
                         template.method(), template.url());
            }
        };
    }

    /**
     * Generate JWT token with role="store" for Bank service authentication
     * 
     * @return JWT token string
     */
    private String generateBankToken() {
        long nowMillis = System.currentTimeMillis();
        // Token valid for 24 hours
        Date expiration = new Date(nowMillis + 24 * 60 * 60 * 1000);

        return JWT.create()
                .withSubject("store-service")
                .withClaim("role", "store")
                .withIssuedAt(new Date(nowMillis))
                .withExpiresAt(expiration)
                .sign(Algorithm.HMAC256(jwtSecret));
    }
}


