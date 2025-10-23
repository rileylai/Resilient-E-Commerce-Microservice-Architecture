package com.tut2.group3.bank.config;

import jakarta.annotation.PostConstruct;
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

    /**
     * Shared signing secret for HS256 JWT validation.
     */
    private String secret;

    /**
     * Header name carrying the bearer token.
     */
    private String header = HttpHeaders.AUTHORIZATION;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("jwt.secret must be configured for JWT validation");
        }
    }

    public String getSecret() {
        return StringUtils.trimWhitespace(secret);
    }
}
