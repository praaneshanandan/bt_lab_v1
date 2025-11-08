package com.app.account.repository;

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
}
