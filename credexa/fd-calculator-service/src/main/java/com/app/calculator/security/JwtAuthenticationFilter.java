package com.app.calculator.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.common.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Authentication Filter for FD Calculator Service (Lab L10)
 * Intercepts requests and validates JWT tokens from Login Service
 * Extracts user context for audit tracking and authorization
 * Enables CUSTOMER role restriction for FD calculations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Skip JWT validation for public endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/health") || 
            requestPath.contains("/swagger") || 
            requestPath.contains("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if Authorization header is present and valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in request headers for path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            jwt = authHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);

            // If username is extracted and no authentication is set
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate token
                if (jwtUtil.validateToken(jwt)) {
                    // Extract roles from JWT
                    List<String> roles = jwtUtil.extractRoles(jwt);
                    
                    // Convert roles to GrantedAuthority
                    List<GrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                    username, 
                                    null, 
                                    authorities
                            );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT authentication successful for user: {} with roles: {} on path: {}", 
                             username, roles, requestPath);
                } else {
                    log.warn("Invalid JWT token for path: {}", requestPath);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
