package com.app.fdaccount.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lab L15 specific withdrawal response DTO
 * Matches the exact specification from Lab L15 document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {

    private String status;              // "success" or "failure"
    private String message;             // Descriptive message
    private BigDecimal withdrawalAmount; // Total amount paid out
    private BigDecimal penaltyApplied;   // Penalty deducted
    
    // Additional details for transparency
    private String fdAccountNo;
    private BigDecimal principalAmount;
    private BigDecimal interestEarned;
    private BigDecimal tdsDeducted;
    private String transactionReference;
}
