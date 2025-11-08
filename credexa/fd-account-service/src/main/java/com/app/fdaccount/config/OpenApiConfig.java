package com.app.fdaccount.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for FD Account Service
 * Configures Swagger UI with JWT Bearer Token Authentication
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fdAccountServiceAPI() {
        // Define the security scheme name
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("FD Account Service API")
                        .description("Fixed Deposit Account Management Service - Handles FD account creation, transactions, maturity processing, and reporting")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Credexa Bank")
                                .email("support@credexa.com")
                                .url("https://credexa.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8086/api/fd-accounts")
                                .description("Local Development Server")
                ))
                // Add security requirement globally
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // Define security scheme
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from login-service. Example: Login with credentials and copy the token from response.")));
    }
}
