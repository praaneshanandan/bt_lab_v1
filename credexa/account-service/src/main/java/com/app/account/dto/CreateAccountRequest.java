package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a Fixed Deposit Account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new FD account")
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 200, message = "Account name must not exceed 200 characters")
    @Schema(description = "Name of the FD account", example = "John Doe FD Account", required = true)
    private String accountName;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID from customer-service", example = "1", required = true)
    private Long customerId;

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    @Schema(description = "Product code from product-pricing-service", example = "FD-STD-001", required = true)
    private String productCode;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.0", message = "Minimum principal amount is ₹1000")
    @Schema(description = "Principal/deposit amount", example = "50000", required = true)
    private BigDecimal principalAmount;

    @NotNull(message = "Term in months is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    @Schema(description = "FD term in months", example = "12", required = true)
    private Integer termMonths;

    @NotNull(message = "Effective date is required")
    @Schema(description = "Account start date", example = "2025-11-08", required = true)
    private LocalDate effectiveDate;

    @Size(max = 50, message = "Branch code must not exceed 50 characters")
    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    @Size(max = 200, message = "Branch name must not exceed 200 characters")
    @Schema(description = "Branch name", example = "Main Branch")
    private String branchName;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Schema(description = "Additional remarks", example = "Regular FD for 1 year")
    private String remarks;

    @Schema(description = "Created by username", example = "manager01")
    private String createdBy;
}
