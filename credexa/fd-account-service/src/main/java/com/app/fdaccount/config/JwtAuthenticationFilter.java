package com.app.fdaccount.config;

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
 * JWT Authentication Filter for fd-account-service
 * Validates JWT tokens and extracts user authentication information
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        log.info("🚀 JwtAuthenticationFilter called for: {} {}", request.getMethod(), request.getRequestURI());
        
        // Skip JWT validation for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("⏭️ Skipping OPTIONS request");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Skip JWT validation for public endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.contains("/actuator") || 
            requestPath.contains("/swagger") || 
            requestPath.contains("/api-docs")) {
            log.info("⏭️ Skipping public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        // Check if Authorization header is present and valid
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("⚠️ No JWT token found in request headers for path: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token
            jwt = authHeader.substring(7);
            log.info("🔍 Received JWT token: {}...", jwt.substring(0, Math.min(20, jwt.length())));
            
            username = jwtUtil.extractUsername(jwt);
            log.info("🔍 Extracted username: {}", username);

            // If username is extracted and no authentication is set
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate token
                if (jwtUtil.validateToken(jwt, username)) {
                    // Extract roles from JWT
                    List<String> roles = jwtUtil.extractRoles(jwt);
                    log.info("🔍 Extracted roles: {}", roles);
                    
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
                    
                    log.info("✅ JWT authentication successful for user: {} with roles: {}", username, roles);
                } else {
                    log.error("❌ Invalid JWT token for path: {}", requestPath);
                }
            }
        } catch (Exception e) {
            log.error("❌ JWT authentication failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
