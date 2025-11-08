package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Redemption Process
 * Contains complete redemption details and transaction information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redemption process response with transaction details")
public class RedemptionProcessResponse {

    // Redemption Details
    @Schema(description = "Redemption transaction ID", example = "TXN-20251108120000-1001")
    private String redemptionTransactionId;

    @Schema(description = "Redemption status", example = "COMPLETED")
    private String redemptionStatus;

    @Schema(description = "Redemption type processed", example = "FULL")
    private String redemptionType;

    // Account Details
    @Schema(description = "Account number", example = "FD-20251108120000-1234-5")
    private String accountNumber;

    @Schema(description = "Account name", example = "John Doe")
    private String accountName;

    @Schema(description = "Account status after redemption", example = "CLOSED")
    private String accountStatus;

    // Financial Details
    @Schema(description = "Principal amount", example = "100000.00")
    private BigDecimal principalAmount;

    @Schema(description = "Interest earned till redemption", example = "15000.00")
    private BigDecimal interestEarned;

    @Schema(description = "TDS deducted", example = "1500.00")
    private BigDecimal tdsDeducted;

    @Schema(description = "Penalty amount charged", example = "750.00")
    private BigDecimal penaltyAmount;

    @Schema(description = "Gross redemption amount (before deductions)", example = "115000.00")
    private BigDecimal grossRedemptionAmount;

    @Schema(description = "Net redemption amount paid", example = "112750.00")
    private BigDecimal netRedemptionAmount;

    @Schema(description = "Balance after redemption (for PARTIAL)", example = "0.00")
    private BigDecimal balanceAfter;

    // Transaction Details
    @Schema(description = "Transaction reference", example = "PAY-2025110812345")
    private String paymentReference;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Redemption date and time", example = "2025-11-08T12:00:00")
    private LocalDateTime redemptionDate;

    @Schema(description = "Processed by user", example = "manager1")
    private String processedBy;

    @Schema(description = "Transaction channel", example = "BRANCH")
    private String channel;

    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    // Calculation Breakdown
    @Schema(description = "Calculation breakdown details")
    private CalculationBreakdown breakdown;

    @Schema(description = "Additional remarks", example = "Full redemption processed successfully")
    private String remarks;

    @Schema(description = "Success message", example = "Redemption processed successfully. Net amount: ₹112,750.00")
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationBreakdown {
        @Schema(description = "Balance before redemption", example = "100000.00")
        private BigDecimal balanceBefore;

        @Schema(description = "Interest credited", example = "15000.00")
        private BigDecimal interestAmount;

        @Schema(description = "TDS deducted", example = "1500.00")
        private BigDecimal tdsAmount;

        @Schema(description = "Penalty charged", example = "750.00")
        private BigDecimal penaltyAmount;

        @Schema(description = "Net amount payable", example = "112750.00")
        private BigDecimal netAmount;

        @Schema(description = "Penalty applicable?", example = "true")
        private Boolean penaltyApplicable;

        @Schema(description = "Penalty reason", example = "Premature redemption before maturity")
        private String penaltyReason;
    }
}
