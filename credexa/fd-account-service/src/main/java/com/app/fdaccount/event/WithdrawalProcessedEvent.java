package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a premature withdrawal is processed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalProcessedEvent {
    
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
    
    // Withdrawal Details
    private LocalDate withdrawalDate;
    private String withdrawalReason;
    private Boolean isPremature;
    
    // Financial Details
    private BigDecimal principalAmount;
    private BigDecimal interestEarned;
    private BigDecimal penaltyAmount;
    private BigDecimal penaltyPercentage;
    private BigDecimal netAmount;
    
    // Account Status
    private Integer daysCompleted;
    private Integer termDays;
    private String accountStatusAfter; // CLOSED
    
    // Additional Info
    private String processedBy;
    private String transactionReference;
}
