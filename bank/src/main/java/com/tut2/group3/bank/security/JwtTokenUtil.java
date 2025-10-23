package com.tut2.group3.bank.security;

import com.tut2.group3.bank.config.JwtConfig;
import com.tut2.group3.bank.exception.InvalidJwtTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.lang.Strings;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility for parsing and validating JWT tokens using the HS256 algorithm.
 * <p>
 * This component encapsulates token validation to keep security filters lightweight and reusable.
 */
@Component
public class JwtTokenUtil {

    private final JwtParser jwtParser;

    public JwtTokenUtil(JwtConfig jwtConfig) {
        Objects.requireNonNull(jwtConfig, "JwtConfig must not be null");
        String secret = jwtConfig.getSecret();

        if (!Strings.hasText(secret)) {
            throw new IllegalStateException("JWT secret must be provided via application properties");
        }

        SecretKey signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build();
    }

    /**
     * Parses the given token and returns the JWT claims if validation succeeds.
     *
     * @param token the raw JWT token string
     * @return parsed claims from the token body
     * @throws InvalidJwtTokenException if the token is null, blank, or fails signature validation
     */
    public Claims parseClaims(String token) throws InvalidJwtTokenException {
        String sanitizedToken = sanitizeToken(token);
        try {
            return jwtParser.parseClaimsJws(sanitizedToken).getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            String detail = Strings.hasText(ex.getMessage()) ? ex.getMessage() : "token parsing failed";
            throw new InvalidJwtTokenException("JWT invalid: " + detail, ex);
        }
    }

    /**
     * Extracts the subject (sub) claim from the provided token.
     *
     * @param token the raw JWT token string
     * @return the subject embedded in the token
     * @throws InvalidJwtTokenException if the token is invalid or lacks a subject
     */
    public String extractSubject(String token) throws InvalidJwtTokenException {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();

        if (!Strings.hasText(subject)) {
            throw new InvalidJwtTokenException("JWT token does not contain a subject");
        }

        return subject;
    }

    /**
     * Extracts the expiration (exp) claim from the provided token.
     *
     * @param token the raw JWT token string
     * @return optional expiration date if present in the token
     * @throws InvalidJwtTokenException if the token is invalid
     */
    public Optional<Date> extractExpiration(String token) throws InvalidJwtTokenException {
        Claims claims = parseClaims(token);
        Date expiration = claims.getExpiration();
        return Optional.ofNullable(expiration);
    }

    private String sanitizeToken(String token) throws InvalidJwtTokenException {
        if (!Strings.hasText(token)) {
            throw new InvalidJwtTokenException("JWT token must not be null or blank");
        }
        return token.trim();
    }

    public String extractClaim(String token, String claimName) throws InvalidJwtTokenException {
        Claims claims = parseClaims(token);
        String value = claims.get(claimName, String.class);

        if (!Strings.hasText(value)) {
            throw new InvalidJwtTokenException("JWT token missing claim: " + claimName);
        }
        return value;
    }
}
