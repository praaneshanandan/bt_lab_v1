package com.app.account.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.app.account.dto.external.CustomerDto;
import com.app.common.dto.ApiResponse;

import reactor.core.publisher.Mono;

/**
 * Client for customer-service integration
 */
@Component
public class CustomerServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceClient.class);

    private final WebClient webClient;
    private final String customerServiceUrl;

    public CustomerServiceClient(WebClient.Builder webClientBuilder,
                                  @Value("${integration.customer-service.url}") String customerServiceUrl) {
        this.customerServiceUrl = customerServiceUrl;
        this.webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
    }

    /**
     * Get customer details by ID
     */
    public CustomerDto getCustomerById(Long customerId) {
        logger.info("🔍 Fetching customer details for ID: {} from URL: {}/{}", customerId, customerServiceUrl, customerId);

        try {
            CustomerDto customer = webClient.get()
                    .uri("/{id}", customerId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), 
                            clientResponse -> {
                                logger.error("❌ 4xx error from customer-service: {} {}", clientResponse.statusCode(), clientResponse.statusCode().value());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            logger.error("❌ 4xx Error Response Body: {}", body);
                                            return Mono.error(new RuntimeException("Customer service returned " + clientResponse.statusCode() + ": " + body));
                                        });
                            })
                    .onStatus(status -> status.is5xxServerError(),
                            clientResponse -> {
                                logger.error("❌ 5xx error from customer-service: {} {}", clientResponse.statusCode(), clientResponse.statusCode().value());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            logger.error("❌ 5xx Error Response Body: {}", body);
                                            return Mono.error(new RuntimeException("Customer service error " + clientResponse.statusCode() + ": " + body));
                                        });
                            })
                    .bodyToMono(CustomerDto.class)
                    .doOnError(error -> logger.error("❌ WebClient error before response: {}", error.getMessage()))
                    .block();

            if (customer != null) {
                logger.info("✅ Customer details fetched successfully: {}", customer.getFullName());
                return customer;
            } else {
                logger.error("❌ Customer not found or null response for ID: {}", customerId);
                throw new RuntimeException("Customer not found with ID: " + customerId);
            }
        } catch (Exception e) {
            logger.error("❌ Error fetching customer with ID {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch customer details: " + e.getMessage(), e);
        }
    }

    /**
     * Validate if customer exists
     */
    public boolean validateCustomer(Long customerId) {
        try {
            CustomerDto customer = getCustomerById(customerId);
            return customer != null;
        } catch (Exception e) {
            logger.error("❌ Customer validation failed for ID: {}", customerId);
            return false;
        }
    }

    /**
     * Get customer by username
     */
    public CustomerDto getCustomerByUsername(String username) {
        logger.info("🔍 Fetching customer by username: {}", username);

        try {
            CustomerDto customer = webClient.get()
                    .uri("/username/{username}", username)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), 
                            clientResponse -> {
                                logger.error("❌ 4xx error from customer-service: {} {}", clientResponse.statusCode(), clientResponse.statusCode().value());
                                return clientResponse.bodyToMono(String.class)
                                        .flatMap(body -> {
                                            logger.error("❌ 4xx Error Response Body: {}", body);
                                            return Mono.error(new RuntimeException("Customer service returned " + clientResponse.statusCode() + ": " + body));
                                        });
                            })
                    .bodyToMono(CustomerDto.class)
                    .block();

            if (customer != null) {
                logger.info("✅ Customer fetched successfully: {}", customer.getFullName());
                return customer;
            } else {
                logger.error("❌ Customer not found for username: {}", username);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Error fetching customer by username {}: {}", username, e.getMessage());
            return null;
        }
    }
}
