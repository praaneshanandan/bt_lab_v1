package com.app.account.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fixed Deposit Account Entity
 * Simplified version with denormalized data for performance
 */
@Entity
@Table(name = "fd_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String accountNumber;

    @Column(unique = true, length = 50)
    private String ibanNumber;

    @Column(nullable = false, length = 200)
    private String accountName;

    // Customer Information (denormalized from customer-service)
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 200)
    private String customerName;

    @Column(length = 100)
    private String customerEmail;

    @Column(length = 20)
    private String customerMobile;

    // Product Information (denormalized from product-pricing-service)
    @Column(nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 50)
    private String productType;

    // Financial Details
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal interestEarned;

    // Dates
    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Column
    private LocalDate closureDate;

    // Calculation Details (from calculator-service)
    @Column(length = 50)
    private String calculationType;

    @Column(length = 50)
    private String compoundingFrequency;

    // TDS
    @Column(precision = 5, scale = 2)
    private BigDecimal tdsRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal tdsAmount;

    @Column
    private Boolean tdsApplicable;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    // Branch
    @Column(length = 50)
    private String branchCode;

    @Column(length = 200)
    private String branchName;

    // Additional Fields
    @Column(length = 500)
    private String remarks;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Account Status Enum
     */
    public enum AccountStatus {
        ACTIVE,
        MATURED,
        CLOSED,
        SUSPENDED
    }
}
