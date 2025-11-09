package com.app.account.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.account.entity.FdAccount;

/**
 * Repository for FD Account operations
 */
@Repository
public interface FdAccountRepository extends JpaRepository<FdAccount, Long> {

    /**
     * Find account by account number
     */
    Optional<FdAccount> findByAccountNumber(String accountNumber);

    /**
     * Find account by IBAN number
     */
    Optional<FdAccount> findByIbanNumber(String ibanNumber);

    /**
     * Find all accounts for a customer
     */
    Page<FdAccount> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Find accounts by status
     */
    Page<FdAccount> findByStatus(FdAccount.AccountStatus status, Pageable pageable);

    /**
     * Check if account number exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find all accounts by status (for batch processing)
     */
    List<FdAccount> findByStatus(FdAccount.AccountStatus status);

    /**
     * Find accounts by status and calculation type (for batch processing)
     */
    List<FdAccount> findByStatusAndCalculationType(FdAccount.AccountStatus status, String calculationType);

    /**
     * Find accounts by status and maturity date less than or equal to (for batch processing)
     */
    List<FdAccount> findByStatusAndMaturityDateLessThanEqual(FdAccount.AccountStatus status, LocalDate maturityDate);
}
