package com.app.product.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",     // All localhost ports
            "https://*.ngrok-free.app", // ngrok domains
            "https://*.ngrok-free.dev",
            "https://*.ngrok.io"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * High-priority CORS filter that runs BEFORE Spring Security
     * Handles OPTIONS preflight requests immediately without authentication
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> corsFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                
                String origin = request.getHeader("Origin");
                if (origin != null && (
                    origin.startsWith("http://localhost:") ||
                    origin.matches("https://.*\\.ngrok-free\\.(app|dev)") ||
                    origin.matches("https://.*\\.ngrok\\.io"))) {
                    
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                    response.setHeader("Access-Control-Allow-Headers", "*");
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                    response.setHeader("Access-Control-Max-Age", "3600");
                    response.setHeader("Access-Control-Expose-Headers", "Authorization");
                }
                
                // For OPTIONS requests, return 200 immediately without further processing
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                }
                
                filterChain.doFilter(request, response);
            }
        });
        
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Run BEFORE Spring Security
        return registrationBean;
    }
}
