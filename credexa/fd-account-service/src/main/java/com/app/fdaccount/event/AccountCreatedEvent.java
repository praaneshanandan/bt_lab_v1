package com.app.fdaccount.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a new FD account is created
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {
    
    private String eventId;
    private String eventType;
    private LocalDateTime timestamp;
    
    // Account Details
    private Long accountId;
    private String accountNumber;
    private String iban;
    private String accountName;
    
    // Customer Details
    private Long customerId;
    private String customerName;
    
    // Product Details
    private String productCode;
    private String productName;
    
    // Financial Details
    private BigDecimal principalAmount;
    private Integer termMonths;
    private BigDecimal interestRate;
    private String interestCalculationMethod;
    
    // Dates
    private LocalDate effectiveDate;
    private LocalDate maturityDate;
    private BigDecimal maturityAmount;
    
    // Additional Info
    private String maturityInstruction;
    private String branchCode;
    private String createdBy;
}
