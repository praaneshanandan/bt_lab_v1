package com.app.customer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Client for calling login-service APIs
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LoginServiceClient {

    private final RestTemplate restTemplate;

    @Value("${login-service.url:http://localhost:8082/api/auth}")
    private String loginServiceUrl;

    /**
     * Get user ID by username from login-service
     * Returns null if user not found (instead of throwing exception)
     */
    public Long getUserIdByUsername(String username) {
        try {
            String url = loginServiceUrl + "/user/" + username;
            log.debug("Calling login-service to get userId for username: {}", username);

            ApiResponse response = restTemplate.getForObject(url, ApiResponse.class);

            if (response != null && response.isSuccess() && response.getData() != null && response.getData().getId() != null) {
                log.debug("Retrieved userId: {} for username: {}", response.getData().getId(), username);
                return response.getData().getId();
            }

            log.warn("User not found in login-service: {}", username);
            return null;  // Return null if user not found

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.info("User {} does not exist in login-service (404)", username);
            return null;
        } catch (Exception e) {
            log.error("Error calling login-service for username: {}", username, e);
            throw new RuntimeException("Error communicating with login-service: " + e.getMessage());
        }
    }

    /**
     * Create user account in login-service (admin only)
     * This is called when admin creates a customer profile and the user doesn't exist yet
     */
    public CreateUserResponse createUserAccount(CreateUserRequest request, String adminJwtToken) {
        try {
            String url = loginServiceUrl + "/admin/create-user";
            log.info("Creating user account in login-service for username: {}", request.getUsername());

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + adminJwtToken);
            headers.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<CreateUserRequest> entity =
                new org.springframework.http.HttpEntity<>(request, headers);

            org.springframework.http.ResponseEntity<CreateUserResponse> response =
                restTemplate.postForEntity(url, entity, CreateUserResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("User account created successfully for username: {}", request.getUsername());
                return response.getBody();
            }

            log.error("Failed to create user account for username: {}", request.getUsername());
            throw new RuntimeException("Failed to create user account in login-service");

        } catch (Exception e) {
            log.error("Error creating user account for username: {}", request.getUsername(), e);
            throw new RuntimeException("Error creating user account: " + e.getMessage());
        }
    }

    /**
     * Inner class for API response wrapper from login-service
     */
    @lombok.Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private UserData data;
        private String timestamp;
    }

    /**
     * Inner class for user data from login-service
     */
    @lombok.Data
    public static class UserData {
        private Long id;  // This is the userId
        private String username;
        private String email;
        private String mobileNumber;
    }

    /**
     * Request DTO for creating user account in login-service
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateUserRequest {
        private String username;
        private String email;
        private String mobileNumber;
        private String preferredLanguage;
        private String preferredCurrency;
        private String temporaryPassword;  // Optional - will be auto-generated if null
    }

    /**
     * Response DTO from creating user account
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreateUserResponse {
        private Long userId;
        private String username;
        private String email;
        private String mobileNumber;
        private String temporaryPassword;  // The generated password
        private String message;
    }
}
