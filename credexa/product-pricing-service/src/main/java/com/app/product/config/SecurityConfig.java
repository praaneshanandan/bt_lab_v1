package com.app.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.app.product.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration for product-pricing-service
 * Enforces JWT-based authentication and role-based authorization (Lab L9)
 * 
 * Lab L9 Enhancement: Secure integration with Login System
 * - JWT token validation required for all endpoints
 * - Role-based authorization (ADMIN, MANAGER, CUSTOMER)
 * - Method-level security with @PreAuthorize annotations
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (health check, swagger, actuator)
                .requestMatchers(
                    "/actuator/**",
                    "/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Read-only endpoints - any authenticated user
                .requestMatchers(
                    "/active",
                    "/currently-active",
                    "/code/**",
                    "/{id}"
                ).authenticated()
                // All other product endpoints require authentication
                // Specific role checks handled by @PreAuthorize in controller
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
