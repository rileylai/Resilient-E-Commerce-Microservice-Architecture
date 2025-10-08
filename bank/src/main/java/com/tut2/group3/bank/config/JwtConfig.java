package com.tut2.group3.bank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private static final String DEFAULT_SECRET = "jD8nFz7eA9hQ2LmBt4KxVwR1zTYuE3gH";

    /**
     * Shared signing secret for HS256 JWT validation.
     */
    private String secret = DEFAULT_SECRET;

    /**
     * Header name carrying the bearer token.
     */
    private String header = HttpHeaders.AUTHORIZATION;

    public String getSecret() {
        if (!StringUtils.hasText(secret)) {
            return DEFAULT_SECRET;
        }
        return secret;
    }
}
