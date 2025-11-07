package com.app.fdaccount.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for partial withdrawal inquiry/execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialWithdrawalResponse {

    private String accountNumber;
    private BigDecimal withdrawalAmount;
    private BigDecimal principalBalanceBefore;
    private BigDecimal principalBalanceAfter;
    private BigDecimal minBalanceRequired;
    private LocalDate withdrawalDate;
    private String transactionReference;
    private Boolean isEligible;
    private String message;
}
