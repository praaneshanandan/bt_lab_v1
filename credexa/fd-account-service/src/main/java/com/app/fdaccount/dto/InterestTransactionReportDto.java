package com.app.fdaccount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Interest Transaction History Report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Interest Transaction History - Daily interest credits")
public class InterestTransactionReportDto {

    @Schema(description = "Account number", example = "FD202411070001")
    private String accountNumber;

    @Schema(description = "Account name", example = "My Fixed Deposit")
    private String accountName;

    @Schema(description = "Transaction reference", example = "TXN-20241107-ABC12345")
    private String transactionReference;

    @Schema(description = "Transaction type", example = "INTEREST_CREDIT")
    private String transactionType;

    @Schema(description = "Interest amount credited", example = "20.55")
    private BigDecimal amount;

    @Schema(description = "Transaction date", example = "2024-11-07")
    private LocalDate transactionDate;

    @Schema(description = "Value date", example = "2024-11-07")
    private LocalDate valueDate;

    @Schema(description = "Principal balance after transaction", example = "100000.00")
    private BigDecimal principalBalanceAfter;

    @Schema(description = "Interest balance after transaction", example = "2520.55")
    private BigDecimal interestBalanceAfter;

    @Schema(description = "Total balance after transaction", example = "102520.55")
    private BigDecimal totalBalanceAfter;

    @Schema(description = "Transaction description")
    private String description;
}
