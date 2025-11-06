package com.app.calculator.service;

import com.app.calculator.dto.external.CustomerDto;
import com.app.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service for integrating with customer-service (Lab L10)
 * Fetches customer categories for personalized FD interest rates
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerIntegrationService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.customer.url}")
    private String customerServiceUrl;
    
    /**
     * Get customer classification (cached)
     */
    @Cacheable(value = "customerClassifications", key = "#customerId")
    public String getCustomerClassification(Long customerId) {
        log.info("Fetching customer classification for ID: {}", customerId);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
            
            ApiResponse<CustomerDto> response = webClient.get()
                .uri("/{id}", customerId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CustomerDto>>() {})
                .block();
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                String classification = response.getData().getCustomerClassification();
                log.debug("Customer {} has classification: {}", customerId, classification);
                return classification;
            }
            
            log.warn("Customer not found with ID: {}", customerId);
            return null;
        } catch (Exception e) {
            log.error("Failed to fetch customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Lab L10: Get customer categories by username from customer-service
     * Returns list of categories (EMPLOYEE, SENIOR_CITIZEN, PREMIUM_CUSTOMER, etc.)
     * for applying additional interest rates
     */
    @Cacheable(value = "customerCategories", key = "#username")
    public List<String> getCustomerCategoriesByUsername(String username) {
        log.info("Lab L10: Fetching customer categories for username: {}", username);
        
        try {
            WebClient webClient = webClientBuilder.baseUrl(customerServiceUrl).build();
            
            // Fetch customer profile by username
            ApiResponse<CustomerDto> response = webClient.get()
                .uri("/profile")
                .header("X-Username", username) // Pass username for customer service to identify
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<CustomerDto>>() {})
                .block();
            
            if (response != null && response.isSuccess() && response.getData() != null) {
                CustomerDto customer = response.getData();
                List<String> categories = new ArrayList<>();
                
                // Extract classification if available
                if (customer.getCustomerClassification() != null && 
                    !customer.getCustomerClassification().isEmpty()) {
                    categories.add(customer.getCustomerClassification());
                }
                
                // Check for additional categories (age-based, employment-based, etc.)
                // These would come from customer entity if implemented
                
                log.info("Lab L10: User {} has categories: {}", username, categories);
                return categories;
            }
            
            log.warn("Lab L10: Customer profile not found for username: {}", username);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Lab L10: Failed to fetch customer categories for {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }
}
