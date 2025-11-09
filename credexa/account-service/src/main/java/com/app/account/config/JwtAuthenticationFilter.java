package com.app.account.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.common.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter - validates JWT tokens for secured endpoints
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        logger.debug("🚀 JwtAuthenticationFilter called for: {} {}", request.getMethod(), request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("❌ No JWT token found in Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);
            logger.debug("🔍 JWT token received: {}...", jwt.substring(0, Math.min(jwt.length(), 20)));

            String username = jwtUtil.extractUsername(jwt);
            logger.debug("🔍 Extracted username: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtil.validateToken(jwt, username)) {
                    logger.debug("✅ JWT token is valid for user: {}", username);

            // Extract roles from JWT
            List<String> roles = jwtUtil.extractRoles(jwt);
            logger.debug("🔍 Extracted roles: {}", roles);

            // Convert roles to GrantedAuthority
            List<SimpleGrantedAuthority> authorities = roles == null ?
                new ArrayList<>() :
                roles.stream()
                 .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                 .collect(Collectors.toList());

            // Create authentication token with authorities
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                username, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("✅ Security context set for user: {}", username);
                } else {
                    logger.error("❌ JWT token validation failed for user: {}", username);
                }
            }
        } catch (Exception e) {
            logger.error("❌ JWT authentication error: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
