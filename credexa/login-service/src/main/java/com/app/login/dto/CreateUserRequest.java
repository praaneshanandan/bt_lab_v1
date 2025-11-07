package com.app.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating user account only (no customer profile)
 * Used when customer-service needs to create a login account for a new customer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 15, message = "Mobile number should be between 10 and 15 digits")
    private String mobileNumber;

    @Builder.Default
    private String preferredLanguage = "en";

    @Builder.Default
    private String preferredCurrency = "INR";

    // Optional - if null, will be auto-generated
    private String temporaryPassword;
}
