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
        logger.info("🔍 Fetching customer details for ID: {}", customerId);

        try {
            ApiResponse<CustomerDto> response = webClient.get()
                    .uri("/customers/{id}", customerId)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<ApiResponse<CustomerDto>>() {})
                    .block();

            if (response != null && response.isSuccess() && response.getData() != null) {
                logger.info("✅ Customer details fetched successfully: {}", response.getData().getFullName());
                return response.getData();
            } else {
                logger.error("❌ Customer not found or invalid response for ID: {}", customerId);
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
}
