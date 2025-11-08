package com.app.fdaccount.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebClient Configuration for inter-service communication
 * Automatically adds JWT token to outgoing requests
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> {
                    // Get the JWT token from the current HTTP request
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    
                    if (attributes != null) {
                        HttpServletRequest httpRequest = attributes.getRequest();
                        String authHeader = httpRequest.getHeader("Authorization");
                        
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            // Add the JWT token to the outgoing request
                            return next.exchange(
                                org.springframework.web.reactive.function.client.ClientRequest.from(request)
                                    .header("Authorization", authHeader)
                                    .build()
                            );
                        }
                    }
                    
                    log.warn("No JWT token available for request to: {}", request.url());
                    return next.exchange(request);
                });
    }
}
