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
 * Interest Payout Service
 * Handles periodic interest payouts to customer accounts
 * When interest is not capitalized, it is paid out to the customer
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterestPayoutService {

    private final FdAccountRepository accountRepository;
    private final InterestService interestService;

    /**
     * Process interest payout for an account
     * This credits interest to a separate account (not added to principal)
     * 
     * @param accountNumber FD Account number
     * @param performedBy User performing the operation
     * @return Updated account
     */
    @Transactional
    public FdAccount processInterestPayout(String accountNumber, String performedBy) {
        log.info("Processing interest payout for account: {}", accountNumber);
        
        // Find account
        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        // Check if account is active
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalStateException("Cannot process payout for non-active account");
        }
        
        // Get current balances
        BigDecimal currentPrincipal = getCurrentBalance(account, "PRINCIPAL");
        BigDecimal currentInterest = getCurrentBalance(account, "INTEREST_ACCRUED");
        
        // Check if there's interest to payout
        if (currentInterest.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("No interest to payout for account: {}", accountNumber);
            return account;
        }
        
        LocalDate today = LocalDate.now();
        
        // Create payout transaction
        AccountTransaction transaction = AccountTransaction.builder()
                .transactionReference(generateTransactionReference())
                .transactionType(TransactionType.INTEREST_CREDIT)
                .amount(currentInterest)
                .transactionDate(today)
                .valueDate(today)
                .description("Interest payout - Credited to customer account")
                .principalBalanceAfter(currentPrincipal)
                .interestBalanceAfter(BigDecimal.ZERO)  // Reset interest balance
                .totalBalanceAfter(currentPrincipal)
                .performedBy(performedBy)
                .isReversed(false)
                .build();
        
        account.addTransaction(transaction);
        
        // Update balances
        account.addBalance(AccountBalance.builder()
                .balanceType("INTEREST_ACCRUED")
                .balance(BigDecimal.ZERO)
                .asOfDate(today)
                .description("Interest reset after payout")
                .build());
        
        account.addBalance(AccountBalance.builder()
                .balanceType("AVAILABLE")
                .balance(currentPrincipal)
                .asOfDate(today)
                .description("Total balance after interest payout")
                .build());
        
        // Save
        accountRepository.save(account);
        
        log.info("✅ Interest paid out for account: {} - Amount: {}", accountNumber, currentInterest);
        
        // In a real system, you would:
        // 1. Credit the interest to the customer's savings/current account
        // 2. Send notification to customer
        // 3. Generate tax documents if applicable
        
        return account;
    }

    /**
     * Process payout for accounts based on payout frequency
     * This is called by the batch job
     * 
     * @param account FD Account
     * @param today Current date
     * @return true if payout was processed
     */
    @Transactional
    public boolean processPayoutIfDue(FdAccount account, LocalDate today) {
        String payoutFrequency = account.getInterestPayoutFrequency();
        
        // If payout frequency is ON_MATURITY, don't payout until maturity
        if ("ON_MATURITY".equals(payoutFrequency)) {
            log.debug("Account {} has ON_MATURITY payout - No periodic payout", account.getAccountNumber());
            return false;
        }
        
        // If account has capitalization enabled, don't payout
        // (Check based on product configuration - for now we assume CAPITALIZE means no payout)
        if ("CAPITALIZE".equals(account.getInterestCalculationMethod())) {
            log.debug("Account {} has capitalization - No payout needed", account.getAccountNumber());
            return false;
        }
        
        // Check if payout is due based on frequency
        boolean isDue = isPayoutDue(account, today, payoutFrequency);
        
        if (isDue) {
            log.info("Payout due for account: {} (Frequency: {})", account.getAccountNumber(), payoutFrequency);
            processInterestPayout(account.getAccountNumber(), "SYSTEM-BATCH");
            return true;
        }
        
        return false;
    }

    /**
     * Check if payout is due based on frequency
     */
    private boolean isPayoutDue(FdAccount account, LocalDate today, String frequency) {
        LocalDate effectiveDate = account.getEffectiveDate();
        
        switch (frequency) {
            case "MONTHLY":
                // Payout on the same day each month
                return today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "QUARTERLY":
                // Payout every 3 months
                int monthsSinceEffective = today.getMonthValue() - effectiveDate.getMonthValue() +
                        (today.getYear() - effectiveDate.getYear()) * 12;
                return monthsSinceEffective % 3 == 0 && today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "HALF_YEARLY":
                // Payout every 6 months
                monthsSinceEffective = today.getMonthValue() - effectiveDate.getMonthValue() +
                        (today.getYear() - effectiveDate.getYear()) * 12;
                return monthsSinceEffective % 6 == 0 && today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            case "YEARLY":
                // Payout on anniversary
                return today.getMonthValue() == effectiveDate.getMonthValue() &&
                        today.getDayOfMonth() == effectiveDate.getDayOfMonth();
                
            default:
                return false;
        }
    }

    /**
     * Queue interest for payout
     * This creates a pending payout record that can be processed in bulk
     * 
     * @param accountNumber FD Account number
     * @param amount Interest amount to payout
     * @return Transaction reference
     */
    public String queueInterestPayout(String accountNumber, BigDecimal amount) {
        log.info("Queuing interest payout for account: {} - Amount: {}", accountNumber, amount);
        
        // In a real system, this would:
        // 1. Create a pending payout record in a separate table
        // 2. Add to a queue/batch for processing
        // 3. Return a tracking reference
        
        String reference = generateTransactionReference();
        log.info("Interest payout queued with reference: {}", reference);
        
        return reference;
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
        return "TXN-PAY-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
