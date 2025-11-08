package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when interest is accrued on an FD account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestAccruedEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    // Account Details
    private Long accountId;
    private String accountNumber;
    
    // Customer Details
    private Long customerId;
    
    // Interest Details
    private BigDecimal interestAmount;
    private LocalDate accrualDate;
    private BigDecimal interestRate;
    private String calculationMethod;
    
    // Balance Details
    private BigDecimal principalBalance;
    private BigDecimal totalInterestAccrued;
    private BigDecimal totalBalance;
    
    // Additional Info
    private Integer daysCompleted;
    private Integer totalDays;
}
