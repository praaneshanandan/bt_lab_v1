package com.app.login.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for admin creating a customer with login account
 * Admin creates both user account and customer profile in one operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateCustomerRequest {

    // ===== Login Account Fields =====
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 15, message = "Mobile number should be between 10 and 15 digits")
    private String mobileNumber;

    @lombok.Builder.Default
    private String preferredLanguage = "en";

    @lombok.Builder.Default
    private String preferredCurrency = "INR";

    // Password will be auto-generated and sent to customer via email/SMS
    // Or admin can optionally provide a temporary password
    @Size(min = 8, message = "Temporary password must be at least 8 characters")
    private String temporaryPassword;

    // ===== Customer Profile Fields =====
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN format (e.g., ABCDE1234F)")
    private String panNumber;

    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhar number must be 12 digits")
    private String aadharNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private String gender; // MALE, FEMALE, OTHER

    @NotNull(message = "Customer classification is required")
    private String classification; // REGULAR, PREMIUM, VIP, SENIOR_CITIZEN, SUPER_SENIOR

    // Address details
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    // Financial details (optional)
    @Size(max = 50)
    private String accountNumber;

    @Size(max = 20)
    private String ifscCode;

    // Communication preferences (optional)
    @lombok.Builder.Default
    private Boolean emailNotifications = true;
    @lombok.Builder.Default
    private Boolean smsNotifications = true;
}
