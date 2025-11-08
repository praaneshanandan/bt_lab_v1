package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.app.account.entity.FdAccount.AccountStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for FD Account details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FD Account details response")
public class AccountResponse {

    @Schema(description = "Account internal ID", example = "1")
    private Long id;

    @Schema(description = "Unique account number", example = "FD-2025-001")
    private String accountNumber;

    @Schema(description = "IBAN account number", example = "IN29CRED0001FD2025110001")
    private String ibanNumber;

    @Schema(description = "Account name", example = "John Doe FD Account")
    private String accountName;

    @Schema(description = "Customer ID", example = "1")
    private Long customerId;

    @Schema(description = "Customer full name", example = "John Doe")
    private String customerName;

    @Schema(description = "Customer email", example = "john.doe@example.com")
    private String customerEmail;

    @Schema(description = "Customer mobile", example = "+919876543210")
    private String customerMobile;

    @Schema(description = "Product code", example = "FD-STD-001")
    private String productCode;

    @Schema(description = "Product name", example = "Standard Fixed Deposit")
    private String productName;

    @Schema(description = "Product type", example = "FIXED_DEPOSIT")
    private String productType;

    @Schema(description = "Principal amount", example = "50000")
    private BigDecimal principalAmount;

    @Schema(description = "Interest rate (annual %)", example = "7.5")
    private BigDecimal interestRate;

    @Schema(description = "Term in months", example = "12")
    private Integer termMonths;

    @Schema(description = "Maturity amount", example = "53750.00")
    private BigDecimal maturityAmount;

    @Schema(description = "Interest earned", example = "3750.00")
    private BigDecimal interestEarned;

    @Schema(description = "Account effective date", example = "2025-11-08")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @Schema(description = "Account maturity date", example = "2026-11-08")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    @Schema(description = "Account closure date", example = "2026-11-08")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate closureDate;

    @Schema(description = "Calculation type", example = "SIMPLE")
    private String calculationType;

    @Schema(description = "Compounding frequency", example = "QUARTERLY")
    private String compoundingFrequency;

    @Schema(description = "TDS rate (%)", example = "10.0")
    private BigDecimal tdsRate;

    @Schema(description = "TDS amount deducted", example = "375.00")
    private BigDecimal tdsAmount;

    @Schema(description = "Whether TDS is applicable", example = "true")
    private Boolean tdsApplicable;

    @Schema(description = "Account status", example = "ACTIVE")
    private AccountStatus status;

    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    @Schema(description = "Branch name", example = "Main Branch")
    private String branchName;

    @Schema(description = "Remarks", example = "Regular FD for 1 year")
    private String remarks;

    @Schema(description = "Created by username", example = "manager01")
    private String createdBy;

    @Schema(description = "Updated by username", example = "admin01")
    private String updatedBy;

    @Schema(description = "Created timestamp", example = "2025-11-08T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp", example = "2025-11-08T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
