package com.app.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

/**
 * WebClient configuration for service-to-service communication with JWT token forwarding
 */
@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .filter((request, next) -> {
                    // Get current request to extract JWT token
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest httpRequest = attributes.getRequest();
                        String authHeader = httpRequest.getHeader("Authorization");
                        
                        logger.info("🔐 WebClient Filter - Auth Header: {}", authHeader != null ? "Present" : "Missing");
                        
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            logger.info("✅ Forwarding JWT token to downstream service: {}", request.url());
                            // Forward JWT token to downstream service - create mutable request
                            ClientRequest modifiedRequest = ClientRequest.from(request)
                                    .header("Authorization", authHeader)
                                    .build();
                            return next.exchange(modifiedRequest);
                        } else {
                            logger.warn("⚠️ No valid Authorization header found, proceeding without JWT");
                        }
                    } else {
                        logger.warn("⚠️ RequestContextHolder attributes are null");
                    }
                    return next.exchange(request);
                });
    }
}
