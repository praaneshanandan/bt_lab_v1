package com.app.account.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

/**
 * WebClient configuration for service-to-service communication with JWT token forwarding
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> {
                    // Get current request to extract JWT token
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest httpRequest = attributes.getRequest();
                        String authHeader = httpRequest.getHeader("Authorization");
                        
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            // Forward JWT token to downstream service
                            request.headers().set("Authorization", authHeader);
                        }
                    }
                    return next.exchange(request);
                });
    }
}
