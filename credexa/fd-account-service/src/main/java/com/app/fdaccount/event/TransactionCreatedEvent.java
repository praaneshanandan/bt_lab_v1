package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a transaction is created on an FD account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    // Account Details
    private Long accountId;
    private String accountNumber;
    
    // Customer Details
    private Long customerId;
    
    // Transaction Details
    private Long transactionId;
    private String transactionReference;
    private String transactionType;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private String description;
    
    // Balance Details
    private BigDecimal principalBalanceAfter;
    private BigDecimal interestBalanceAfter;
    private BigDecimal totalBalanceAfter;
    
    // Additional Info
    private String performedBy;
    private Boolean isReversed;
}
