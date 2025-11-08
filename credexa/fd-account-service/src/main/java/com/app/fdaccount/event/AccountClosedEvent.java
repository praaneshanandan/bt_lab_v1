package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when an FD account is closed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountClosedEvent {
    
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
    
    // Closure Details
    private LocalDate closureDate;
    private String closureReason;
    private String closureType; // MATURITY, PREMATURE, MANUAL
    
    // Financial Details
    private BigDecimal principalAmount;
    private BigDecimal interestEarned;
    private BigDecimal penaltyAmount;
    private BigDecimal netPayoutAmount;
    
    // Additional Info
    private Integer daysCompleted;
    private Integer totalDays;
    private String closedBy;
}
