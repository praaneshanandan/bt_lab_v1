package com.app.login.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for calling customer-service APIs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceClient {

    private final RestTemplate restTemplate;

    @Value("${microservices.customer-service.url:http://localhost:8083/api/customer}")
    private String customerServiceUrl;

    /**
     * Create customer profile in customer-service
     * This is an inter-service call, so we use internal authentication
     */
    public CustomerProfileResponse createCustomerProfile(CreateCustomerProfileRequest request, String jwtToken) {
        try {
            String url = customerServiceUrl;
            log.info("=== ATTEMPTING TO CREATE CUSTOMER PROFILE ===");
            log.info("URL: {}", url);
            log.info("Username: {}", request.getUsername());
            log.info("Full Name: {}", request.getFullName());
            log.info("JWT Token: {}...", jwtToken != null ? jwtToken.substring(0, Math.min(20, jwtToken.length())) : "NULL");

            // Set Authorization header with JWT token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            headers.set("Content-Type", "application/json");

            HttpEntity<CreateCustomerProfileRequest> entity = new HttpEntity<>(request, headers);

            log.info("Calling POST {} with Authorization header", url);
            ResponseEntity<CustomerProfileResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                CustomerProfileResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✓ Customer profile created successfully for user: {}", request.getUsername());
                return response.getBody();
            }

            log.error("✗ Failed to create customer profile for user: {}. Status: {}", 
                    request.getUsername(), response.getStatusCode());
            throw new RuntimeException("Failed to create customer profile in customer-service");

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("✗ HTTP Error calling customer-service for user: {}", request.getUsername());
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Exception: ", e);
            throw new RuntimeException("HTTP Error calling customer-service: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("✗ Unexpected error calling customer-service for user: {}", request.getUsername(), e);
            throw new RuntimeException("Error communicating with customer-service: " + e.getMessage());
        }
    }

    /**
     * Request DTO for creating customer profile
     * Must match CreateCustomerRequest structure from customer-service
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateCustomerProfileRequest {
        private String username;  // Will be set from authenticated user
        private String fullName;
        private String mobileNumber;
        private String email;
        private String panNumber;
        private String aadharNumber;
        private java.time.LocalDate dateOfBirth;
        private String gender;  // String will be converted to enum by customer-service
        private String classification;  // String will be converted to enum by customer-service
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String pincode;
        private String country;
        private String accountNumber;
        private String ifscCode;
        private String preferredLanguage;
        private String preferredCurrency;
        private Boolean emailNotifications;
        private Boolean smsNotifications;
    }

    /**
     * Response DTO from customer-service
     * Must match CustomerResponse structure from customer-service
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CustomerProfileResponse {
        private Long id;
        private Long userId;
        private String username;
        private String fullName;
        private String mobileNumber;
        private String email;
        private String panNumber;
        private String aadharNumber;
        private java.time.LocalDate dateOfBirth;
        private String gender; // Will be enum string like "MALE", "FEMALE", "OTHER"
        private String classification; // Will be enum string like "REGULAR", "PREMIUM", etc.
        private String kycStatus; // Will be enum string like "PENDING", "VERIFIED", etc.
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String pincode;
        private String country;
        private Boolean isActive;
        private String accountNumber;
        private String ifscCode;
        private String preferredLanguage;
        private String preferredCurrency;
        private Boolean emailNotifications;
        private Boolean smsNotifications;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }
}
