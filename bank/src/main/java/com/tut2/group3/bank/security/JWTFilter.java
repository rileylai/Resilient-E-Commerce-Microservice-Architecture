package com.tut2.group3.bank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.config.JwtConfig;
import com.tut2.group3.bank.exception.InvalidJwtTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<RequestMatcher> PROTECTED_ENDPOINTS = List.of(
            new AntPathRequestMatcher("/api/bank/debit"),
            new AntPathRequestMatcher("/api/bank/refund")
    );

    private final JwtTokenUtil jwtTokenUtil;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String headerName = StringUtils.hasText(jwtConfig.getHeader()) ? jwtConfig.getHeader() : HttpHeaders.AUTHORIZATION;
        String authorizationHeader = request.getHeader(headerName);
        // debug only !!!
        log.debug("[DEBUG] Authorization Header = {}", authorizationHeader);

        String token = extractBearerToken(authorizationHeader);
        if (!StringUtils.hasText(token)) {
            log.debug("JWT missing Authorization header for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
            return;
        }

        try {
            String subject = jwtTokenUtil.extractSubject(token);
            String role = jwtTokenUtil.extractClaim(token, "role");

            // Set authentication in SecurityContext, so that downstream can use it, e.g., in controllers
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(subject, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authToken);

            // debug
            log.debug("[DEBUG] JWT subject (caller) = {}", subject);
            log.debug("[DEBUG] JWT role = {}", role);

            // only "store" role is allowed to access the protected endpoints
            if (!"store".equals(role)) {
                log.warn("Forbidden: JWT role={} is not allowed to access {}", role, request.getRequestURI());
                writeForbidden(response);
                return;
            }

            log.info("Validated JWT subject={} with role={} for {} {}", subject, role, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } catch (InvalidJwtTokenException ex) {
            log.warn("Invalid JWT for request to {}: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response);
        }
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.error(ErrorCode.FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PROTECTED_ENDPOINTS.stream().noneMatch(matcher -> matcher.matches(request));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.error(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }
}
