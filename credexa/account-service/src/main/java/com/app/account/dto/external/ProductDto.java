package com.app.account.dto.external;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Product data from product-pricing-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("baseInterestRate")
    @Schema(description = "Base interest rate")
    private BigDecimal baseInterestRate;

    @JsonProperty("interestCalculationMethod")
    @Schema(description = "Interest calculation method")
    private String interestCalculationMethod;

    @JsonProperty("tdsApplicable")
    @Schema(description = "Whether TDS is applicable")
    private Boolean tdsApplicable;

    @JsonProperty("tdsRate")
    @Schema(description = "TDS rate")
    private BigDecimal tdsRate;

    @JsonProperty("allowedRoles")
    @Schema(description = "Roles allowed to create this product")
    private String allowedRoles;

    @JsonProperty("active")
    @Schema(description = "Whether product is active")
    private Boolean active;
}
