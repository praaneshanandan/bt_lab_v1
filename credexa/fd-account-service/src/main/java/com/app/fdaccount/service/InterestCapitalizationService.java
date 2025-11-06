package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.fdaccount.entity.AccountBalance;
import com.app.fdaccount.entity.AccountTransaction;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.enums.TransactionType;
import com.app.fdaccount.exception.AccountNotFoundException;
import com.app.fdaccount.repository.FdAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Interest Capitalization Service
 * Handles the process of capitalizing interest (adding interest to principal)
 * This is typically done based on the product's interest payout frequency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestCapitalizationService {

    private final FdAccountRepository accountRepository;
    private final InterestService interestService;

    /**
     * Capitalize accrued interest to principal
     * This adds the accumulated interest to the principal amount
     * 
     * @param accountNumber FD Account number
     * @param performedBy User performing the operation
     * @return Updated account
     */
    @Transactional
    public FdAccount capitalizeInterest(String accountNumber, String performedBy) {
        log.info("Capitalizing interest for account: {}", accountNumber);
        
        // Find account
        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        // Check if account is active
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalStateException("Cannot capitalize interest for non-active account");
        }
        
        // Get current balances
        BigDecimal currentPrincipal = getCurrentBalance(account, "PRINCIPAL");
        BigDecimal currentInterest = getCurrentBalance(account, "INTEREST_ACCRUED");
        
        // Check if there's interest to capitalize
        if (currentInterest.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("No interest to capitalize for account: {}", accountNumber);
            return account;
        }
        
        // New principal = Current principal + Interest
        BigDecimal newPrincipal = currentPrincipal.add(currentInterest);
        
        LocalDate today = LocalDate.now();
        
        // Create capitalization transaction
        AccountTransaction transaction = AccountTransaction.builder()
                .transactionReference(generateTransactionReference())
                .transactionType(TransactionType.INTEREST_CAPITALIZATION)
                .amount(currentInterest)
                .transactionDate(today)
                .valueDate(today)
                .description("Interest capitalization - Added to principal")
                .principalBalanceAfter(newPrincipal)
                .interestBalanceAfter(BigDecimal.ZERO)  // Reset interest balance
                .totalBalanceAfter(newPrincipal)
                .performedBy(performedBy)
                .isReversed(false)
                .build();
        
        account.addTransaction(transaction);
        
        // Update balances
        account.addBalance(AccountBalance.builder()
                .balanceType("PRINCIPAL")
                .balance(newPrincipal)
                .asOfDate(today)
                .description("Principal after interest capitalization")
                .build());
        
        account.addBalance(AccountBalance.builder()
                .balanceType("INTEREST_ACCRUED")
                .balance(BigDecimal.ZERO)
                .asOfDate(today)
                .description("Interest reset after capitalization")
                .build());
        
        account.addBalance(AccountBalance.builder()
                .balanceType("AVAILABLE")
                .balance(newPrincipal)
                .asOfDate(today)
                .description("Total balance after capitalization")
                .build());
        
        // Update principal amount in account
        account.setPrincipalAmount(newPrincipal);
        
        // Save
        accountRepository.save(account);
        
        log.info("✅ Interest capitalized for account: {} - Amount: {}, New Principal: {}", 
                accountNumber, currentInterest, newPrincipal);
        
        return account;
    }

    /**
     * Process capitalization for accounts based on payout frequency
     * This is called by the batch job
     * 
     * @param account FD Account
     * @param today Current date
     * @return true if capitalization was performed
     */
    @Transactional
    public boolean processCapitalizationIfDue(FdAccount account, LocalDate today) {
        String payoutFrequency = account.getInterestPayoutFrequency();
        
        // If payout frequency is ON_MATURITY, accumulate interest until maturity
        if ("ON_MATURITY".equals(payoutFrequency)) {
            log.debug("Account {} has ON_MATURITY payout - No capitalization needed", account.getAccountNumber());
            return false;
        }
        
        // Check if capitalization is due based on frequency
        boolean isDue = isCapitalizationDue(account, today, payoutFrequency);
        
        if (isDue) {
            log.info("Capitalization due for account: {} (Frequency: {})", account.getAccountNumber(), payoutFrequency);
            capitalizeInterest(account.getAccountNumber(), "SYSTEM-BATCH");
            return true;
        }
        
        return false;
    }

    /**
     * Check if capitalization is due based on frequency
     */
    private boolean isCapitalizationDue(FdAccount account, LocalDate today, String frequency) {
        LocalDate effectiveDate = account.getEffectiveDate();
        
        switch (frequency) {
            case "MONTHLY":
                // Capitalize on the same day each month
                return today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "QUARTERLY":
                // Capitalize every 3 months
                int monthsSinceEffective = today.getMonthValue() - effectiveDate.getMonthValue() +
                        (today.getYear() - effectiveDate.getYear()) * 12;
                return monthsSinceEffective % 3 == 0 && today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "HALF_YEARLY":
                // Capitalize every 6 months
                monthsSinceEffective = today.getMonthValue() - effectiveDate.getMonthValue() +
                        (today.getYear() - effectiveDate.getYear()) * 12;
                return monthsSinceEffective % 6 == 0 && today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "YEARLY":
                // Capitalize on anniversary
                return today.getMonthValue() == effectiveDate.getMonthValue() &&
                        today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            default:
                return false;
        }
    }

    /**
     * Get current balance for a balance type
     */
    private BigDecimal getCurrentBalance(FdAccount account, String balanceType) {
        return account.getBalances().stream()
                .filter(b -> balanceType.equals(b.getBalanceType()))
                .max((b1, b2) -> b1.getAsOfDate().compareTo(b2.getAsOfDate()))
                .map(AccountBalance::getBalance)
                .orElse(balanceType.equals("PRINCIPAL") ? account.getPrincipalAmount() : BigDecimal.ZERO);
    }

    /**
     * Generate unique transaction reference
     */
    private String generateTransactionReference() {
        return "TXN-CAP-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
