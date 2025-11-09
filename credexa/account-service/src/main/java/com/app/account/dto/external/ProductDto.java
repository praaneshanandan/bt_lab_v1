package com.app.account.dto.external;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Product data from product-pricing-service
 * Matches ProductResponse structure from product-pricing-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Product details from product-pricing-service")
public class ProductDto {

    @JsonProperty("productId")
    @Schema(description = "Product ID")
    private Long productId;

    @JsonProperty("productCode")
    @Schema(description = "Product code")
    private String productCode;

    @JsonProperty("productName")
    @Schema(description = "Product name")
    private String productName;

    @JsonProperty("productType")
    @Schema(description = "Product type")
    private String productType;

    @JsonProperty("description")
    @Schema(description = "Product description")
    private String description;

    @JsonProperty("effectiveDate")
    @Schema(description = "Effective date")
    private LocalDate effectiveDate;

    @JsonProperty("bankBranchCode")
    @Schema(description = "Bank branch code")
    private String bankBranchCode;

    @JsonProperty("currencyCode")
    @Schema(description = "Currency code")
    private String currencyCode;

    @JsonProperty("status")
    @Schema(description = "Product status")
    private String status;

    @JsonProperty("minAmount")
    @Schema(description = "Minimum deposit amount")
    private BigDecimal minAmount;

    @JsonProperty("maxAmount")
    @Schema(description = "Maximum deposit amount")
    private BigDecimal maxAmount;

    @JsonProperty("minTermMonths")
    @Schema(description = "Minimum term in months")
    private Integer minTermMonths;

    @JsonProperty("maxTermMonths")
    @Schema(description = "Maximum term in months")
    private Integer maxTermMonths;

    @JsonProperty("minBalanceRequired")
    @Schema(description = "Minimum balance required")
    private BigDecimal minBalanceRequired;

    @JsonProperty("baseInterestRate")
    @Schema(description = "Base interest rate")
    private BigDecimal baseInterestRate;

    @JsonProperty("interestCalculationMethod")
    @Schema(description = "Interest calculation method")
    private String interestCalculationMethod;

    @JsonProperty("interestPayoutFrequency")
    @Schema(description = "Interest payout frequency")
    private String interestPayoutFrequency;

    @JsonProperty("prematureWithdrawalAllowed")
    @Schema(description = "Premature withdrawal allowed")
    private Boolean prematureWithdrawalAllowed;

    @JsonProperty("partialWithdrawalAllowed")
    @Schema(description = "Partial withdrawal allowed")
    private Boolean partialWithdrawalAllowed;

    @JsonProperty("loanAgainstDepositAllowed")
    @Schema(description = "Loan against deposit allowed")
    private Boolean loanAgainstDepositAllowed;

    @JsonProperty("autoRenewalAllowed")
    @Schema(description = "Auto renewal allowed")
    private Boolean autoRenewalAllowed;

    @JsonProperty("nomineeAllowed")
    @Schema(description = "Nominee allowed")
    private Boolean nomineeAllowed;

    @JsonProperty("jointAccountAllowed")
    @Schema(description = "Joint account allowed")
    private Boolean jointAccountAllowed;

    @JsonProperty("tdsApplicable")
    @Schema(description = "Whether TDS is applicable")
    private Boolean tdsApplicable;

    @JsonProperty("tdsRate")
    @Schema(description = "TDS rate")
    private BigDecimal tdsRate;

    @JsonProperty("allowedRoles")
    @Schema(description = "Roles allowed to create this product")
    private List<ProductRoleDto> allowedRoles;  // Changed from String to List

    @JsonProperty("charges")
    @Schema(description = "Product charges")
    private List<Object> charges;  // Simplified - not used in account creation

    @JsonProperty("interestRateMatrix")
    @Schema(description = "Interest rate matrix")
    private List<Object> interestRateMatrix;  // Simplified - not used in account creation

    @JsonProperty("currentlyActive")
    @Schema(description = "Whether product is currently active")
    private Boolean currentlyActive;

    @JsonProperty("createdAt")
    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Updated timestamp")
    private LocalDateTime updatedAt;

    @JsonProperty("createdBy")
    @Schema(description = "Created by")
    private String createdBy;

    @JsonProperty("updatedBy")
    @Schema(description = "Updated by")
    private String updatedBy;

    /**
     * Nested DTO for Product Role
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductRoleDto {
        private Long id;
        private String roleType;
        private String description;
    }
}
