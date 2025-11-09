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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Batch job for daily interest accrual
 * Disabled by default - set batch.interest-accrual.enabled=true to enable
 */
@Component
@ConditionalOnProperty(name = "batch.interest-accrual.enabled", havingValue = "true", matchIfMissing = false)
public class InterestAccrualBatch {

    private static final Logger logger = LoggerFactory.getLogger(InterestAccrualBatch.class);

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired
    private BatchTimeService batchTimeService;

    /**
     * Run daily at 1:00 AM (or manually triggered)
     */
    @Scheduled(cron = "${batch.interest-accrual.cron:0 0 1 * * ?}")
    public void processInterestAccrual() {
        LocalDate batchDate = batchTimeService.getBatchDate();
        logger.info("🕐 Starting Interest Accrual Batch for date: {}", batchDate);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        try {
            // Get all active FD accounts
            List<FdAccount> activeAccounts = accountRepository.findByStatus(FdAccount.AccountStatus.ACTIVE);
            logger.info("📊 Found {} active accounts for interest accrual", activeAccounts.size());

            for (FdAccount account : activeAccounts) {
                try {
                    // Process each account in its own transaction
                    boolean processed = processAccountAccrual(account, batchDate);
                    if (processed) {
                        successCount++;
                    } else {
                        skipCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    logger.error("❌ Error processing interest accrual for account {}: {}", 
                            account.getAccountNumber(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Interest Accrual Batch completed in {}ms - Success: {}, Skipped: {}, Errors: {}", 
                    duration, successCount, skipCount, errorCount);

        } catch (Exception e) {
            logger.error("❌ Interest Accrual Batch failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Process accrual for a single account (in its own transaction)
     */
    @Transactional
    private boolean processAccountAccrual(FdAccount account, LocalDate batchDate) {
        // Skip if account effective date is after batch date
        if (account.getEffectiveDate().isAfter(batchDate)) {
            return false;
        }

        // Skip if account maturity date is before batch date
        if (account.getMaturityDate().isBefore(batchDate)) {
            return false;
        }

        // Check if already accrued for this date (idempotency check)
        if (isAlreadyAccruedForDate(account, batchDate)) {
            logger.debug("⏭️ Skipping account {} - already accrued for date {}", 
                    account.getAccountNumber(), batchDate);
            return false;
        }

        // Calculate daily interest
        BigDecimal dailyInterest = calculateDailyInterest(account, batchDate);
        
        if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
            // Create interest accrual transaction (this doesn't credit to account yet)
            createInterestAccrualTransaction(account, dailyInterest, batchDate);
            logger.debug("✅ Accrued interest {} for account {}", dailyInterest, account.getAccountNumber());
            return true;
        }
        
        return false;
    }

    /**
     * Check if interest has already been accrued for this date (idempotency check)
     */
    private boolean isAlreadyAccruedForDate(FdAccount account, LocalDate date) {
        // Check if an accrual transaction exists for this exact date
        List<FdTransaction> transactions = transactionRepository
                .findByAccountAndTransactionTypeAndTransactionDateBetween(
                        account,
                        FdTransaction.TransactionType.INTEREST_CREDIT,
                        date.atStartOfDay(),
                        date.atTime(23, 59, 59)
                );
        
        // Check if any transaction is an accrual (not capitalization)
        boolean hasAccrual = transactions.stream()
                .anyMatch(txn -> txn.getRemarks() != null && 
                        txn.getRemarks().contains("Interest accrued but not credited") &&
                        txn.getDescription() != null &&
                        txn.getDescription().contains(date.toString()));
        
        return hasAccrual;
    }

    /**
     * Calculate daily interest for an account
     */
    private BigDecimal calculateDailyInterest(FdAccount account, LocalDate asOfDate) {
        BigDecimal principal = account.getPrincipalAmount();
        BigDecimal annualRate = account.getInterestRate();
        
        // Daily interest = (Principal × Annual Rate) / 365
        BigDecimal dailyRate = annualRate.divide(new BigDecimal("36500"), 10, RoundingMode.HALF_UP);
        BigDecimal dailyInterest = principal.multiply(dailyRate).setScale(2, RoundingMode.HALF_UP);
        
        return dailyInterest;
    }

    /**
     * Create interest accrual transaction (doesn't update account balance)
     */
    private void createInterestAccrualTransaction(FdAccount account, BigDecimal interestAmount, LocalDate accrualDate) {
        FdTransaction transaction = FdTransaction.builder()
                .transactionId(generateTransactionId())
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(FdTransaction.TransactionType.INTEREST_CREDIT)
                .amount(interestAmount)
                .balanceBefore(account.getPrincipalAmount())
                .balanceAfter(account.getPrincipalAmount()) // Balance not updated yet
                .status(FdTransaction.TransactionStatus.COMPLETED)
                .description("Daily interest accrual for " + accrualDate)
                .remarks("BATCH: Interest accrued but not credited")
                .initiatedBy("SYSTEM-BATCH")
                .approvedBy("SYSTEM-BATCH")
                .transactionDate(accrualDate.atTime(1, 0))
                .approvalDate(accrualDate.atTime(1, 0))
                .valueDate(accrualDate.atTime(1, 0))
                .channel("BATCH")
                .branchCode(account.getBranchCode())
                .build();

        transactionRepository.save(transaction);
    }

    private String generateTransactionId() {
        return "TXN-" + LocalDateTime.now().toString().replaceAll("[:-]", "").substring(0, 17);
    }
}
