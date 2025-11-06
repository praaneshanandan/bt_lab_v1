package com.app.fdaccount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Customer Portfolio Report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer FD Portfolio - Individual account details")
public class FDPortfolioDto {

    @Schema(description = "Account number", example = "FD202411070001")
    private String accountNumber;

    @Schema(description = "Account name", example = "My Fixed Deposit")
    private String accountName;

    @Schema(description = "Product code", example = "FD_STD")
    private String productCode;

    @Schema(description = "Product name", example = "Standard Fixed Deposit")
    private String productName;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;

    @Schema(description = "Principal amount", example = "100000.00")
    private BigDecimal principalAmount;

    @Schema(description = "Interest rate (%)", example = "7.50")
    private BigDecimal interestRate;

    @Schema(description = "Term in months", example = "12")
    private Integer termMonths;

    @Schema(description = "Maturity amount", example = "107500.00")
    private BigDecimal maturityAmount;

    @Schema(description = "Current interest accrued", example = "2500.00")
    private BigDecimal interestAccrued;

    @Schema(description = "Available balance for withdrawal", example = "102500.00")
    private BigDecimal availableBalance;

    @Schema(description = "Effective date", example = "2024-01-15")
    private LocalDate effectiveDate;

    @Schema(description = "Maturity date", example = "2025-01-15")
    private LocalDate maturityDate;

    @Schema(description = "Days to maturity", example = "69")
    private Integer daysToMaturity;

    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    @Schema(description = "Branch name", example = "Main Branch")
    private String branchName;
}
