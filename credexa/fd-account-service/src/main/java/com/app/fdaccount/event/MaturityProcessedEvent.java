package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when an account reaches maturity and is processed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaturityProcessedEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    // Account Details
    private Long accountId;
    private String accountNumber;
    private String accountName;
    
    // Customer Details
    private Long customerId;
    private String customerName;
    
    // Maturity Details
    private LocalDate maturityDate;
    private String maturityInstruction; // CLOSE_AND_PAYOUT, RENEW_PRINCIPAL_ONLY, RENEW_WITH_INTEREST, etc.
    
    // Financial Details
    private BigDecimal principalAmount;
    private BigDecimal interestEarned;
    private BigDecimal totalAmount;
    private BigDecimal payoutAmount;
    private BigDecimal renewalAmount;
    
    // Renewal Details (if applicable)
    private Boolean isRenewed;
    private LocalDate newMaturityDate;
    private Integer newTermMonths;
    
    // Transfer Details (if applicable)
    private String transferToAccount;
    
    // Additional Info
    private String processedBy;
}
