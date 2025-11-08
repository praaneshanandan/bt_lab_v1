package com.app.fdaccount.dto;

import com.app.fdaccount.enums.MaturityInstruction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new FD account with values inherited from the product
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new Fixed Deposit account with product default values")
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name cannot exceed 100 characters")
    @Schema(description = "Display name for the FD account", example = "John Doe Regular FD", required = true)
    private String accountName;

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code cannot exceed 50 characters")
    @Schema(description = "Product code from product-pricing service", example = "FD001", required = true)
    private String productCode;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum principal amount is 1000")
    @DecimalMax(value = "99999999999999999.99", message = "Principal amount exceeds maximum limit")
    @Digits(integer = 17, fraction = 2, message = "Invalid principal amount format")
    @Schema(description = "Principal deposit amount", example = "50000.00", required = true)
    private BigDecimal principalAmount;

    @NotNull(message = "Term in months is required")
    @Min(value = 1, message = "Minimum term is 1 month")
    @Max(value = 1200, message = "Maximum term is 1200 months (100 years)")
    @Schema(description = "Fixed deposit term in months", example = "12", required = true)
    private Integer termMonths;

    @NotNull(message = "Effective date is required")
    @Schema(description = "Account start date", example = "2025-11-08", required = true)
    private LocalDate effectiveDate;

    @NotNull(message = "At least one account role is required")
    @Size(min = 1, message = "At least one account owner/role is required")
    @Schema(description = "List of account roles (owners, co-owners, nominees, etc.)", required = true)
    private List<AccountRoleRequest> roles;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Branch code cannot exceed 20 characters")
    @Schema(description = "Branch identifier", example = "BR001", required = true)
    private String branchCode;

    @Size(max = 100, message = "Branch name cannot exceed 100 characters")
    @Schema(description = "Branch display name (optional)", example = "Main Branch")
    private String branchName;

    // Optional fields - defaults from product if not provided
    @Schema(description = "Whether to auto-renew on maturity", example = "false")
    private Boolean autoRenewal;

    @Schema(description = "What to do when FD matures", example = "CLOSE_AND_PAYOUT")
    private MaturityInstruction maturityInstruction;

    @Size(max = 50, message = "Maturity transfer account cannot exceed 50 characters")
    @Schema(description = "Target account number for maturity transfer (if instruction is TRANSFER_*)", example = "SA20251108001")
    private String maturityTransferAccount;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Schema(description = "Optional notes about the account", example = "Regular FD for 1 year")
    private String remarks;

    @Schema(description = "Whether TDS should be deducted on interest", example = "true")
    private Boolean tdsApplicable;

    @Size(max = 100, message = "Created by cannot exceed 100 characters")
    @Schema(description = "Username of the person creating the account", example = "manager01")
    private String createdBy;
}
