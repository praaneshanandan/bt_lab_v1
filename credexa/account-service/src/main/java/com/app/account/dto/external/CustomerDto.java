package com.app.account.dto.external;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Customer data from customer-service
 * Must match CustomerResponse from customer-service exactly
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer details from customer-service")
public class CustomerDto {

    @JsonProperty("id")
    @Schema(description = "Customer ID")
    private Long id;

    @JsonProperty("userId")
    @Schema(description = "User ID for login")
    private Long userId;

    @JsonProperty("username")
    @Schema(description = "Username")
    private String username;

    @JsonProperty("fullName")
    @Schema(description = "Customer full name")
    private String fullName;

    @JsonProperty("mobileNumber")
    @Schema(description = "Customer mobile number")
    private String mobileNumber;

    @JsonProperty("email")
    @Schema(description = "Customer email")
    private String email;

    @JsonProperty("panNumber")
    @Schema(description = "PAN number")
    private String panNumber;

    @JsonProperty("aadharNumber")
    @Schema(description = "Aadhar number")
    private String aadharNumber;

    @JsonProperty("dateOfBirth")
    @Schema(description = "Date of birth")
    private LocalDate dateOfBirth;

    @JsonProperty("gender")
    @Schema(description = "Gender")
    private String gender;

    @JsonProperty("classification")
    @Schema(description = "Customer classification")
    private String classification;

    @JsonProperty("kycStatus")
    @Schema(description = "KYC verification status")
    private String kycStatus;

    // Address details (flat structure)
    @JsonProperty("addressLine1")
    @Schema(description = "Address line 1")
    private String addressLine1;

    @JsonProperty("addressLine2")
    @Schema(description = "Address line 2")
    private String addressLine2;

    @JsonProperty("city")
    @Schema(description = "City")
    private String city;

    @JsonProperty("state")
    @Schema(description = "State")
    private String state;

    @JsonProperty("pincode")
    @Schema(description = "Pincode")
    private String pincode;

    @JsonProperty("country")
    @Schema(description = "Country")
    private String country;

    // Financial details
    @JsonProperty("isActive")
    @Schema(description = "Is customer active")
    private Boolean isActive;

    @JsonProperty("accountNumber")
    @Schema(description = "Bank account number")
    private String accountNumber;

    @JsonProperty("ifscCode")
    @Schema(description = "IFSC code")
    private String ifscCode;

    // Preferences
    @JsonProperty("preferredLanguage")
    @Schema(description = "Preferred language")
    private String preferredLanguage;

    @JsonProperty("preferredCurrency")
    @Schema(description = "Preferred currency")
    private String preferredCurrency;

    // Communication preferences
    @JsonProperty("emailNotifications")
    @Schema(description = "Email notifications enabled")
    private Boolean emailNotifications;

    @JsonProperty("smsNotifications")
    @Schema(description = "SMS notifications enabled")
    private Boolean smsNotifications;

    // Metadata
    @JsonProperty("createdAt")
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;
}
