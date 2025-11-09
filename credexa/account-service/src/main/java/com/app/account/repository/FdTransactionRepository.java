package com.app.account.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.account.entity.FdTransaction;

/**
 * Repository for FD Transaction operations
 */
@Repository
public interface FdTransactionRepository extends JpaRepository<FdTransaction, Long> {

    /**
     * Find transaction by transaction ID
     */
    Optional<FdTransaction> findByTransactionId(String transactionId);

    /**
     * Find all transactions for an account
     */
    Page<FdTransaction> findByAccountNumber(String accountNumber, Pageable pageable);

    /**
     * Find transactions by account ID
     */
    Page<FdTransaction> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Find transactions by type
     */
    Page<FdTransaction> findByTransactionType(FdTransaction.TransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by status
     */
    Page<FdTransaction> findByStatus(FdTransaction.TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by account number and type
     */
    Page<FdTransaction> findByAccountNumberAndTransactionType(
            String accountNumber, 
            FdTransaction.TransactionType transactionType, 
            Pageable pageable);

    /**
     * Find transactions by account number and status
     */
    Page<FdTransaction> findByAccountNumberAndStatus(
            String accountNumber, 
            FdTransaction.TransactionStatus status, 
            Pageable pageable);

    /**
     * Find transactions by date range
     */
    @Query("SELECT t FROM FdTransaction t WHERE t.accountNumber = :accountNumber " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Page<FdTransaction> findByAccountNumberAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find transactions by reference number
     */
    List<FdTransaction> findByReferenceNumber(String referenceNumber);

    /**
     * Check if transaction ID exists
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Count transactions for account
     */
    long countByAccountNumber(String accountNumber);

    /**
     * Count transactions by account number and type
     */
    long countByAccountNumberAndTransactionType(String accountNumber, FdTransaction.TransactionType transactionType);

    /**
     * Get latest transaction for account
     */
    Optional<FdTransaction> findFirstByAccountNumberOrderByTransactionDateDesc(String accountNumber);

    /**
     * Get latest transaction by account number and type
     */
    Optional<FdTransaction> findFirstByAccountNumberAndTransactionTypeOrderByTransactionDateDesc(
            String accountNumber, FdTransaction.TransactionType transactionType);

    /**
     * Find transactions by account, type, and date range (for batch processing)
     */
    @Query("SELECT t FROM FdTransaction t WHERE t.account = :account " +
           "AND t.transactionType = :transactionType " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<FdTransaction> findByAccountAndTransactionTypeAndTransactionDateBetween(
            @Param("account") com.app.account.entity.FdAccount account,
            @Param("transactionType") FdTransaction.TransactionType transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
