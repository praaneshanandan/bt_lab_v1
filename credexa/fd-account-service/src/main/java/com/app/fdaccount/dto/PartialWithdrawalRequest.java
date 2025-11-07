package com.app.fdaccount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for partial withdrawal from FD account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialWithdrawalRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Withdrawal amount is required")
    @Positive(message = "Withdrawal amount must be positive")
    private BigDecimal withdrawalAmount;

    @NotNull(message = "Withdrawal date is required")
    private LocalDate withdrawalDate;

    private String performedBy;

    private String remarks;
}
