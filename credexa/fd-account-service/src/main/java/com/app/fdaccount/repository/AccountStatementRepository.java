package com.app.fdaccount.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.app.fdaccount.entity.AccountStatement;
import com.app.fdaccount.entity.AccountStatement.StatementType;

/**
 * Repository for AccountStatement entity
 */
@Repository
public interface AccountStatementRepository extends JpaRepository<AccountStatement, Long> {
    
    /**
     * Find all statements for an account
     */
    @Query("SELECT s FROM AccountStatement s WHERE s.account.id = :accountId ORDER BY s.statementDate DESC")
    List<AccountStatement> findByAccountId(@Param("accountId") Long accountId);
    
    /**
     * Find statements by account number
     */
    @Query("SELECT s FROM AccountStatement s WHERE s.account.accountNumber = :accountNumber ORDER BY s.statementDate DESC")
    List<AccountStatement> findByAccountNumber(@Param("accountNumber") String accountNumber);
    
    /**
     * Find statement by reference
     */
    Optional<AccountStatement> findByStatementReference(String statementReference);
    
    /**
     * Find statements by type
     */
    List<AccountStatement> findByStatementTypeOrderByStatementDateDesc(StatementType statementType);
    
    /**
     * Find statements for a date range
     */
    @Query("SELECT s FROM AccountStatement s WHERE s.statementDate BETWEEN :startDate AND :endDate ORDER BY s.statementDate DESC")
    List<AccountStatement> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Check if statement already exists for account and date
     */
    @Query("SELECT COUNT(s) > 0 FROM AccountStatement s WHERE s.account.id = :accountId AND s.statementDate = :statementDate AND s.statementType = :statementType")
    boolean existsByAccountIdAndDateAndType(@Param("accountId") Long accountId, @Param("statementDate") LocalDate statementDate, @Param("statementType") StatementType statementType);
    
    /**
     * Find latest statement for account
     */
    @Query("SELECT s FROM AccountStatement s WHERE s.account.id = :accountId ORDER BY s.statementDate DESC LIMIT 1")
    Optional<AccountStatement> findLatestByAccountId(@Param("accountId") Long accountId);
}
