package com.app.fdaccount.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lab L15 specific withdrawal request DTO
 * Matches the exact specification from Lab L15 document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {

    @NotBlank(message = "FD account number is required")
    private String fdAccountNo;

    @NotNull(message = "Withdrawal date is required")
    private LocalDate withdrawalDate;

    private String transferAccount;  // Optional: savings account for transfer
}
