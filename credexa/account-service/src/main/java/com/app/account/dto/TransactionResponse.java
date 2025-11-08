package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.app.account.entity.FdTransaction.TransactionStatus;
import com.app.account.entity.FdTransaction.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Transaction details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction details response")
public class TransactionResponse {

    @Schema(description = "Transaction internal ID", example = "1")
    private Long id;

    @Schema(description = "Transaction ID", example = "TXN-20251108-001")
    private String transactionId;

    @Schema(description = "Account number", example = "FD-20251108120000-1234-5")
    private String accountNumber;

    @Schema(description = "Account name", example = "John Doe FD Account")
    private String accountName;

    @Schema(description = "Transaction type", example = "INTEREST_CREDIT")
    private TransactionType transactionType;

    @Schema(description = "Transaction amount", example = "5000.00")
    private BigDecimal amount;

    @Schema(description = "Balance before transaction", example = "50000.00")
    private BigDecimal balanceBefore;

    @Schema(description = "Balance after transaction", example = "55000.00")
    private BigDecimal balanceAfter;

    @Schema(description = "Transaction status", example = "COMPLETED")
    private TransactionStatus status;

    @Schema(description = "Reference number", example = "REF-2025-001")
    private String referenceNumber;

    @Schema(description = "Transaction description", example = "Interest credit for Q1 2025")
    private String description;

    @Schema(description = "Remarks", example = "Quarterly interest payment")
    private String remarks;

    @Schema(description = "Initiated by", example = "system")
    private String initiatedBy;

    @Schema(description = "Approved by", example = "admin")
    private String approvedBy;

    @Schema(description = "Transaction date", example = "2025-11-08T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime transactionDate;

    @Schema(description = "Approval date", example = "2025-11-08T10:35:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvalDate;

    @Schema(description = "Value date", example = "2025-11-08T00:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime valueDate;

    @Schema(description = "Transaction channel", example = "ONLINE")
    private String channel;

    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;

    @Schema(description = "IP address", example = "192.168.1.1")
    private String ipAddress;
}
