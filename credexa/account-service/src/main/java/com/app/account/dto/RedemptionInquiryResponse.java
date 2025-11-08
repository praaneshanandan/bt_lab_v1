package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Redemption Inquiry
 * Provides complete details for redemption calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redemption inquiry response with complete calculation details")
public class RedemptionInquiryResponse {

    // Account Details
    @Schema(description = "Internal account ID", example = "1")
    private Long accountId;

    @Schema(description = "FD account number", example = "FD-20251108120000-1234-5")
    private String accountNumber;

    @Schema(description = "IBAN number", example = "IN29CRED0001FD2511081234")
    private String ibanNumber;

    @Schema(description = "Account holder name", example = "John Doe")
    private String accountName;

    @Schema(description = "Account status", example = "ACTIVE")
    private String accountStatus;

    // Customer Details
    @Schema(description = "Customer ID", example = "101")
    private Long customerId;

    @Schema(description = "Customer name", example = "John Doe")
    private String customerName;

    // Product Details
    @Schema(description = "Product code", example = "FD-5YR-SENIOR")
    private String productCode;

    @Schema(description = "Product name", example = "5 Year Senior Citizen FD")
    private String productName;

    // Financial Details
    @Schema(description = "Principal amount", example = "100000.00")
    private BigDecimal principalAmount;

    @Schema(description = "Interest rate per annum", example = "7.50")
    private BigDecimal interestRate;

    @Schema(description = "Term in months", example = "60")
    private Integer termMonths;

    @Schema(description = "Maturity amount (at term completion)", example = "143965.00")
    private BigDecimal maturityAmount;

    // Dates
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "FD start date", example = "2023-11-08")
    private LocalDate effectiveDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "FD maturity date", example = "2028-11-08")
    private LocalDate maturityDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Today's date (redemption inquiry date)", example = "2025-11-08")
    private LocalDate inquiryDate;

    // Tenure Details
    @Schema(description = "Days elapsed since effective date", example = "730")
    private Long daysElapsed;

    @Schema(description = "Days remaining until maturity", example = "1095")
    private Long daysRemaining;

    @Schema(description = "Months elapsed", example = "24")
    private Integer monthsElapsed;

    @Schema(description = "Months remaining", example = "36")
    private Integer monthsRemaining;

    @Schema(description = "Whether FD has reached maturity", example = "false")
    private Boolean isMatured;

    // Redemption Calculation
    @Schema(description = "Current balance (from last transaction or principal)", example = "100000.00")
    private BigDecimal currentBalance;

    @Schema(description = "Interest earned till date", example = "15000.00")
    private BigDecimal interestEarned;

    @Schema(description = "TDS deducted till date", example = "1500.00")
    private BigDecimal tdsDeducted;

    @Schema(description = "Penalty amount (if premature redemption)", example = "750.00")
    private BigDecimal penaltyAmount;

    @Schema(description = "Net redemption amount (balance + interest - TDS - penalty)", example = "112750.00")
    private BigDecimal netRedemptionAmount;

    // Penalty Details
    @Schema(description = "Whether penalty is applicable", example = "true")
    private Boolean penaltyApplicable;

    @Schema(description = "Penalty rate applied", example = "0.50")
    private BigDecimal penaltyRate;

    @Schema(description = "Penalty description", example = "Premature redemption penalty: 0.5% on interest earned")
    private String penaltyDescription;

    // TDS Details
    @Schema(description = "TDS rate applicable", example = "10.00")
    private BigDecimal tdsRate;

    @Schema(description = "Whether TDS is applicable", example = "true")
    private Boolean tdsApplicable;

    // Transaction Summary
    @Schema(description = "Total number of transactions on this account", example = "5")
    private Long totalTransactions;

    @Schema(description = "Number of interest credits", example = "2")
    private Long interestCreditCount;

    @Schema(description = "Number of TDS deductions", example = "2")
    private Long tdsDeductionCount;

    // Redemption Type
    @Schema(description = "Redemption type: PREMATURE, ON_MATURITY, POST_MATURITY", example = "PREMATURE")
    private String redemptionType;

    @Schema(description = "Additional remarks", example = "Premature redemption before maturity date")
    private String remarks;

    // Branch Details
    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    @Schema(description = "Branch name", example = "Main Branch")
    private String branchName;
}
