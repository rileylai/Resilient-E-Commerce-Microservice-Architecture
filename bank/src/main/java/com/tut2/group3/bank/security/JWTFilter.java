package com.tut2.group3.bank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tut2.group3.bank.common.ErrorCode;
import com.tut2.group3.bank.common.Result;
import com.tut2.group3.bank.config.JwtConfig;
import com.tut2.group3.bank.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

    private final JWTUtil jwtUtil;
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

        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            log.debug("JWT missing Authorization header for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        // debug only !!!
        log.debug("[DEBUG] JWT token extracted = {}", token);
        
        if (!StringUtils.hasText(token)) {
            log.debug("JWT token blank after Bearer prefix for {} {}", request.getMethod(), request.getRequestURI());
            writeUnauthorized(response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            String service = claims.getSubject();
            // debug only !!!
            log.debug("[DEBUG] JWT subject (caller) = {}", service);
            
            if (!StringUtils.hasText(service)) {
                log.warn("JWT subject missing for {} {}", request.getMethod(), request.getRequestURI());
                writeUnauthorized(response);
                return;
            }

            // for simplicity, we assign a fixed role. In real scenarios, roles/authorities can be extracted from claims.
            // allow access to /api/bank/** endpoints to all services with valid JWT
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            service,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICE")) // 可根據需求自訂角色
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.setAttribute("caller", service);
            log.info("Authenticated service={} for {} {}", service, request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("JWT validation failed for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response);
        }
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
}
