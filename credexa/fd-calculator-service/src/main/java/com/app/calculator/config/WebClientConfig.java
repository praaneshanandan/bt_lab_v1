package com.app.calculator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WebClient configuration for inter-service communication
 * Automatically forwards JWT tokens from incoming requests to outgoing service calls
 */
@Configuration
@Slf4j
public class WebClientConfig {
    
    private final HttpServletRequest httpServletRequest;
    
    public WebClientConfig(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter(jwtForwardingFilter());
    }
    
    /**
     * Filter to forward JWT token from current request to inter-service calls
     */
    private ExchangeFilterFunction jwtForwardingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            // Get JWT token from current HTTP request
            String authHeader = httpServletRequest.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // Add Authorization header to outgoing request
                ClientRequest newRequest = ClientRequest.from(clientRequest)
                        .header("Authorization", authHeader)
                        .build();
                
                log.debug("Forwarding JWT token to: {}", clientRequest.url());
                return Mono.just(newRequest);
            }
            
            log.debug("No JWT token to forward for: {}", clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
