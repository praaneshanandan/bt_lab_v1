package com.app.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Configuration for serving static resources from Gateway
 * This serves the React frontend built into src/main/resources/static
 */
@Configuration
public class StaticResourceConfig {

    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions
            // Serve specific static assets
            .route(GET("/assets/**"), request -> 
                ServerResponse.ok()
                    .bodyValue(new ClassPathResource("static" + request.path())))
            .andRoute(GET("/bank-logo.png"), request ->
                ServerResponse.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .bodyValue(new ClassPathResource("static/bank-logo.png")))
            .andRoute(GET("/vite.svg"), request ->
                ServerResponse.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .bodyValue(new ClassPathResource("static/vite.svg")))
            // Serve index.html for root and all non-API routes (SPA fallback)
            .andRoute(GET("/"), request ->
                ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(new ClassPathResource("static/index.html")))
            // Fallback for SPA routing - serve index.html for any non-API route
            .andRoute(GET("/{path:[^\\.]*}").and(request -> 
                !request.path().startsWith("/api/") && 
                !request.path().startsWith("/actuator/")), request ->
                ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(new ClassPathResource("static/index.html")));
    }
}
