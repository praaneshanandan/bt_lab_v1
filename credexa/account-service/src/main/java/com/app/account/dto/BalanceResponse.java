package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.app.account.entity.FdAccount.AccountStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Account Balance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account balance details")
public class BalanceResponse {

    @Schema(description = "Account number", example = "FD-2025-001")
    private String accountNumber;

    @Schema(description = "Account name", example = "John Doe FD Account")
    private String accountName;

    @Schema(description = "Principal amount", example = "50000")
    private BigDecimal principalAmount;

    @Schema(description = "Interest earned", example = "3750.00")
    private BigDecimal interestEarned;

    @Schema(description = "Maturity amount", example = "53750.00")
    private BigDecimal maturityAmount;

    @Schema(description = "TDS amount", example = "375.00")
    private BigDecimal tdsAmount;

    @Schema(description = "Net amount after TDS", example = "53375.00")
    private BigDecimal netAmount;

    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus status;

    @Schema(description = "Effective date", example = "2025-11-08")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @Schema(description = "Maturity date", example = "2026-11-08")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    @Schema(description = "Days to maturity", example = "365")
    private Long daysToMaturity;
}
