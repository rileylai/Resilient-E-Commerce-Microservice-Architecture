package com.tut2.group3.warehouse.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tut2.group3.warehouse.common.ErrorCode;
import com.tut2.group3.warehouse.common.Result;
import com.tut2.group3.warehouse.utils.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for health check endpoint
        String path = request.getRequestURI();
        if (path.equals("/api/warehouse/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get token from Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // Validate token
            if (!jwtUtil.validateToken(token)) {
                sendUnauthorizedResponse(response, "Invalid or expired token");
                return;
            }

            // Extract user information and set as request attributes
            String username = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);

            request.setAttribute("username", username);
            request.setAttribute("userId", userId);

            log.debug("Authenticated user: {} (ID: {})", username, userId);

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            sendUnauthorizedResponse(response, "Authentication failed: " + e.getMessage());
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Result<Object> result = Result.error(ErrorCode.UNAUTHORIZED, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
