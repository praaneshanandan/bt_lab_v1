package com.app.fdaccount.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for FD Summary Report (Admin/Bank Officer)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FD Summary Report - Aggregated data by product")
public class FDReportDto {

    @Schema(description = "Product code", example = "FD_STD")
    private String productCode;

    @Schema(description = "Product name", example = "Standard Fixed Deposit")
    private String productName;

    @Schema(description = "Total number of accounts", example = "150")
    private Long totalAccounts;

    @Schema(description = "Total principal amount across all accounts", example = "15000000.00")
    private BigDecimal totalPrincipal;

    @Schema(description = "Total interest accrued across all accounts", example = "425000.00")
    private BigDecimal totalInterestAccrued;

    @Schema(description = "Total maturity amount (principal + interest)", example = "15425000.00")
    private BigDecimal totalMaturityAmount;

    @Schema(description = "Number of active accounts", example = "120")
    private Long activeAccounts;

    @Schema(description = "Number of matured accounts", example = "25")
    private Long maturedAccounts;

    @Schema(description = "Number of closed accounts", example = "5")
    private Long closedAccounts;
}
