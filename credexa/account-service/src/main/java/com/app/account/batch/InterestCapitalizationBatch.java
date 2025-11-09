package com.app.account.batch;

import com.app.account.entity.FdAccount;
import com.app.account.entity.FdTransaction;
import com.app.account.repository.FdAccountRepository;
import com.app.account.repository.FdTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch job for quarterly interest capitalization
 * Disabled by default - set batch.interest-capitalization.enabled=true to enable
 */
@Component
@ConditionalOnProperty(name = "batch.interest-capitalization.enabled", havingValue = "true", matchIfMissing = false)
public class InterestCapitalizationBatch {

    private static final Logger logger = LoggerFactory.getLogger(InterestCapitalizationBatch.class);

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired
    private BatchTimeService batchTimeService;

    /**
     * Run on 1st day of every quarter at 2:00 AM
     */
    @Scheduled(cron = "${batch.interest-capitalization.cron:0 0 2 1 1,4,7,10 ?}")
    public void processInterestCapitalization() {
        LocalDate batchDate = batchTimeService.getBatchDate();
        logger.info("🕐 Starting Interest Capitalization Batch for date: {}", batchDate);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        try {
            // Get all active FD accounts with COMPOUND interest
            List<FdAccount> compoundAccounts = accountRepository.findByStatusAndCalculationType(
                    FdAccount.AccountStatus.ACTIVE, 
                    "COMPOUND"
            );
            
            logger.info("📊 Found {} compound interest accounts for capitalization", compoundAccounts.size());

            for (FdAccount account : compoundAccounts) {
                try {
                    // Process each account in its own transaction
                    boolean processed = processAccountCapitalization(account, batchDate);
                    if (processed) {
                        successCount++;
                    } else {
                        skipCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    logger.error("❌ Error processing interest capitalization for account {}: {}", 
                            account.getAccountNumber(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Interest Capitalization Batch completed in {}ms - Success: {}, Skipped: {}, Errors: {}", 
                    duration, successCount, skipCount, errorCount);

        } catch (Exception e) {
            logger.error("❌ Interest Capitalization Batch failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Process capitalization for a single account (in its own transaction)
     */
    @Transactional
    private boolean processAccountCapitalization(FdAccount account, LocalDate batchDate) {
        // Skip if not capitalization date for this account
        if (!isCapitalizationDate(account, batchDate)) {
            return false;
        }

        // Check if already capitalized for this quarter (idempotency check)
        if (isAlreadyCapitalizedForQuarter(account, batchDate)) {
            logger.info("⏭️ Skipping account {} - already capitalized for this quarter", 
                    account.getAccountNumber());
            return false;
        }

        // Get accrued interest for the quarter (ONLY accrual transactions, not capitalization)
        BigDecimal accruedInterest = getAccruedInterestForQuarter(account, batchDate);
        
        if (accruedInterest.compareTo(BigDecimal.ZERO) > 0) {
            // Capitalize interest (add to principal)
            capitalizeInterest(account, accruedInterest, batchDate);
            logger.info("✅ Capitalized interest {} for account {}", accruedInterest, account.getAccountNumber());
            return true;
        } else {
            logger.info("⏭️ Skipping account {} - no accrued interest for quarter", 
                    account.getAccountNumber());
            return false;
        }
    }

    /**
     * Check if today is a capitalization date for the account
     * Returns true if we're at or past a quarter boundary (3, 6, 9, 12... months)
     * Idempotency check prevents duplicate capitalization
     */
    private boolean isCapitalizationDate(FdAccount account, LocalDate date) {
        LocalDate effectiveDate = account.getEffectiveDate();
        
        // Calculate months between effective date and current date
        long totalMonths = java.time.temporal.ChronoUnit.MONTHS.between(effectiveDate, date);
        
        // Must be at least 3 months
        if (totalMonths < 3) {
            return false;
        }
        
        // Check if we've passed a quarter boundary (3, 6, 9, 12...)
        // We capitalize on or after the quarter anniversary
        long quartersPassed = totalMonths / 3;
        
        if (quartersPassed == 0) {
            return false; // Less than 3 months
        }
        
        // Calculate the most recent quarter date
        LocalDate lastQuarterDate = effectiveDate.plusMonths(quartersPassed * 3);
        
        // Return true if current date is on or after the quarter date
        // Idempotency check will prevent duplicate capitalization if already done
        return !date.isBefore(lastQuarterDate);
    }

    /**
     * Check if interest has already been capitalized for this quarter (idempotency check)
     */
    private boolean isAlreadyCapitalizedForQuarter(FdAccount account, LocalDate date) {
        // Calculate quarter start and end dates
        LocalDate quarterStart = date.minusMonths(3);
        LocalDate quarterEnd = date;
        
        // Check if a capitalization transaction exists for this quarter
        List<FdTransaction> capitalizationTransactions = transactionRepository
                .findByAccountAndTransactionTypeAndTransactionDateBetween(
                        account,
                        FdTransaction.TransactionType.INTEREST_CREDIT,
                        quarterStart.atStartOfDay(),
                        quarterEnd.atTime(23, 59, 59)
                );
        
        // Check if any transaction has capitalization remarks
        boolean hasCapitalization = capitalizationTransactions.stream()
                .anyMatch(txn -> txn.getRemarks() != null && 
                        txn.getRemarks().contains("capitalized and added to principal"));
        
        if (hasCapitalization) {
            logger.info("🔒 Quarter already capitalized for account {} (period: {} to {})", 
                    account.getAccountNumber(), quarterStart, quarterEnd);
        }
        
        return hasCapitalization;
    }

    /**
     * Get accrued interest for the quarter from transactions (ONLY accrual, not capitalization)
     */
    private BigDecimal getAccruedInterestForQuarter(FdAccount account, LocalDate endDate) {
        LocalDate startDate = endDate.minusMonths(3);
        
        // Get all interest credit transactions for the quarter
        List<FdTransaction> interestTransactions = transactionRepository
                .findByAccountAndTransactionTypeAndTransactionDateBetween(
                        account,
                        FdTransaction.TransactionType.INTEREST_CREDIT,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59)
                );

        // Sum ONLY accrual transactions (not capitalization transactions)
        BigDecimal accruedInterest = interestTransactions.stream()
                .filter(txn -> txn.getRemarks() != null && 
                        txn.getRemarks().contains("Interest accrued but not credited"))
                .map(FdTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        logger.info("📊 Found {} accrual transactions totaling {} for account {} (period: {} to {})", 
                interestTransactions.stream()
                    .filter(txn -> txn.getRemarks() != null && 
                            txn.getRemarks().contains("Interest accrued but not credited"))
                    .count(),
                accruedInterest, 
                account.getAccountNumber(), 
                startDate, 
                endDate);
        
        return accruedInterest;
    }

    /**
     * Capitalize interest by adding to principal
     */
    private void capitalizeInterest(FdAccount account, BigDecimal interestAmount, LocalDate capitalizationDate) {
        BigDecimal oldPrincipal = account.getPrincipalAmount();
        BigDecimal newPrincipal = oldPrincipal.add(interestAmount);

        // Update account principal
        account.setPrincipalAmount(newPrincipal);
        accountRepository.save(account);

        // Create capitalization transaction
        FdTransaction transaction = FdTransaction.builder()
                .transactionId(generateTransactionId())
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(FdTransaction.TransactionType.INTEREST_CREDIT)
                .amount(interestAmount)
                .balanceBefore(oldPrincipal)
                .balanceAfter(newPrincipal)
                .status(FdTransaction.TransactionStatus.COMPLETED)
                .description("Quarterly interest capitalization")
                .remarks("BATCH: Interest capitalized and added to principal")
                .initiatedBy("SYSTEM-BATCH")
                .approvedBy("SYSTEM-BATCH")
                .transactionDate(capitalizationDate.atTime(2, 0))
                .approvalDate(capitalizationDate.atTime(2, 0))
                .valueDate(capitalizationDate.atTime(2, 0))
                .channel("BATCH")
                .branchCode(account.getBranchCode())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionId() {
        return "TXN-" + LocalDateTime.now().toString().replaceAll("[:-]", "").substring(0, 17);
    }
}
