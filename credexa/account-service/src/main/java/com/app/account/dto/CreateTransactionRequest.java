package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.app.account.entity.FdTransaction.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new transaction")
public class CreateTransactionRequest {

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Type of transaction", example = "INTEREST_CREDIT", required = true)
    private TransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Transaction amount", example = "5000", required = true)
    private BigDecimal amount;

    @Schema(description = "Reference number", example = "REF-2025-001")
    private String referenceNumber;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Transaction description", example = "Interest credit for Q1 2025")
    private String description;

    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    @Schema(description = "Additional remarks", example = "Quarterly interest payment")
    private String remarks;

    @Schema(description = "Value date for transaction", example = "2025-11-08T10:30:00")
    private LocalDateTime valueDate;

    @Size(max = 50, message = "Channel must not exceed 50 characters")
    @Schema(description = "Transaction channel", example = "ONLINE")
    private String channel;

    @Size(max = 50, message = "Branch code must not exceed 50 characters")
    @Schema(description = "Branch code", example = "BR001")
    private String branchCode;
}
