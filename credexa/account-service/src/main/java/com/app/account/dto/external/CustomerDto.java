package com.app.account.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Customer data from customer-service
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
    private String userId;

    @JsonProperty("fullName")
    @Schema(description = "Customer full name")
    private String fullName;

    @JsonProperty("email")
    @Schema(description = "Customer email")
    private String email;

    @JsonProperty("mobile")
    @Schema(description = "Customer mobile")
    private String mobile;

    @JsonProperty("panNumber")
    @Schema(description = "PAN number")
    private String panNumber;

    @JsonProperty("aadharNumber")
    @Schema(description = "Aadhar number")
    private String aadharNumber;

    @JsonProperty("customerClassification")
    @Schema(description = "Customer classification")
    private String customerClassification;

    @JsonProperty("kycStatus")
    @Schema(description = "KYC verification status")
    private String kycStatus;

    @JsonProperty("address")
    @Schema(description = "Customer address")
    private AddressDto address;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        @JsonProperty("addressLine1")
        private String addressLine1;

        @JsonProperty("addressLine2")
        private String addressLine2;

        @JsonProperty("city")
        private String city;

        @JsonProperty("state")
        private String state;

        @JsonProperty("pinCode")
        private String pinCode;
    }
}
