package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Interest Calculation
 * Contains complete calculation details and transaction information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Interest calculation response with complete details")
public class InterestCalculationResponse {

    // Account Details
    @Schema(description = "Account number", example = "FD-20251108120000-1234-5")
    private String accountNumber;

    @Schema(description = "Account name", example = "John Doe")
    private String accountName;

    @Schema(description = "Account status", example = "ACTIVE")
    private String accountStatus;

    // Calculation Period
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Interest calculation from date", example = "2025-05-08")
    private LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Interest calculation to date", example = "2025-11-08")
    private LocalDate toDate;

    @Schema(description = "Number of days in calculation period", example = "184")
    private Long daysInPeriod;

    // Financial Details
    @Schema(description = "Principal amount", example = "100000.00")
    private BigDecimal principalAmount;

    @Schema(description = "Interest rate per annum", example = "7.50")
    private BigDecimal interestRate;

    @Schema(description = "Calculation type", example = "COMPOUND")
    private String calculationType;

    @Schema(description = "Compounding frequency", example = "QUARTERLY")
    private String compoundingFrequency;

    // Interest Calculation
    @Schema(description = "Interest calculated for the period", example = "3750.00")
    private BigDecimal interestAmount;

    @Schema(description = "TDS rate applicable", example = "10.00")
    private BigDecimal tdsRate;

    @Schema(description = "TDS amount deducted", example = "375.00")
    private BigDecimal tdsAmount;

    @Schema(description = "Net interest after TDS", example = "3375.00")
    private BigDecimal netInterest;

    // Balance Details
    @Schema(description = "Balance before interest credit", example = "100000.00")
    private BigDecimal balanceBefore;

    @Schema(description = "Balance after interest credit", example = "103375.00")
    private BigDecimal balanceAfter;

    // Transaction Details
    @Schema(description = "Whether interest was credited to account", example = "true")
    private Boolean interestCredited;

    @Schema(description = "Whether TDS was deducted", example = "true")
    private Boolean tdsDeducted;

    @Schema(description = "Interest credit transaction ID", example = "TXN-20251108120000-5001")
    private String interestTransactionId;

    @Schema(description = "TDS deduction transaction ID", example = "TXN-20251108120100-5002")
    private String tdsTransactionId;

    // Calculation Breakdown
    @Schema(description = "Detailed calculation breakdown")
    private CalculationBreakdown breakdown;

    // Previous Interest Credits
    @Schema(description = "Total interest credited till date (from all previous transactions)", example = "15000.00")
    private BigDecimal totalInterestCreditedTillDate;

    @Schema(description = "Total TDS deducted till date (from all previous transactions)", example = "1500.00")
    private BigDecimal totalTdsDeductedTillDate;

    @Schema(description = "Number of previous interest credits", example = "4")
    private Long previousInterestCreditsCount;

    // Messages
    @Schema(description = "Calculation message", example = "Interest calculated successfully for 184 days")
    private String message;

    @Schema(description = "Additional remarks", example = "Quarterly interest calculation completed")
    private String remarks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationBreakdown {
        @Schema(description = "Principal amount used", example = "100000.00")
        private BigDecimal principal;

        @Schema(description = "Annual interest rate", example = "7.50")
        private BigDecimal annualRate;

        @Schema(description = "Days in calculation period", example = "184")
        private Long days;

        @Schema(description = "Days in year (for calculation)", example = "365")
        private Integer daysInYear;

        @Schema(description = "Gross interest calculated", example = "3750.00")
        private BigDecimal grossInterest;

        @Schema(description = "TDS applicable?", example = "true")
        private Boolean tdsApplicable;

        @Schema(description = "TDS rate", example = "10.00")
        private BigDecimal tdsRate;

        @Schema(description = "TDS amount", example = "375.00")
        private BigDecimal tdsAmount;

        @Schema(description = "Net interest after TDS", example = "3375.00")
        private BigDecimal netInterest;

        @Schema(description = "Formula used", example = "Simple: (P × R × T) / (100 × 365)")
        private String formula;

        @Schema(description = "Transactions created")
        private List<String> transactionsCreated;
    }
}
