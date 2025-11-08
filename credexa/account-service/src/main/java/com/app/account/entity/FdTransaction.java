package com.app.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FD Transaction Entity
 * Tracks all transactions on FD accounts (deposits, interest credits, withdrawals, closures)
 */
@Entity
@Table(name = "fd_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FdTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private FdAccount account;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceBefore;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(length = 100)
    private String referenceNumber;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String remarks;

    @Column(length = 100)
    private String initiatedBy;

    @Column(length = 100)
    private String approvedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime transactionDate;

    @Column
    private LocalDateTime approvalDate;

    @Column
    private LocalDateTime valueDate;

    @Column(length = 50)
    private String channel;

    @Column(length = 50)
    private String branchCode;

    @Column(length = 100)
    private String ipAddress;

    /**
     * Transaction Type Enum
     */
    public enum TransactionType {
        DEPOSIT,              // Initial deposit/principal
        INTEREST_CREDIT,      // Interest accrual/credit
        TDS_DEDUCTION,        // TDS deducted
        WITHDRAWAL,           // Premature/partial withdrawal
        MATURITY_CREDIT,      // Maturity amount credit
        CLOSURE,              // Account closure
        REVERSAL,             // Transaction reversal
        ADJUSTMENT            // Manual adjustment
    }

    /**
     * Transaction Status Enum
     */
    public enum TransactionStatus {
        PENDING,              // Awaiting approval
        APPROVED,             // Approved and processed
        COMPLETED,            // Successfully completed
        FAILED,               // Transaction failed
        REJECTED,             // Rejected by approver
        REVERSED              // Transaction reversed
    }
}
