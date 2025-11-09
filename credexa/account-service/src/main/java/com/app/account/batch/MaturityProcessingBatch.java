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
 * Batch job for processing matured FD accounts
 * Disabled by default - set batch.maturity-processing.enabled=true to enable
 */
@Component
@ConditionalOnProperty(name = "batch.maturity-processing.enabled", havingValue = "true", matchIfMissing = false)
public class MaturityProcessingBatch {

    private static final Logger logger = LoggerFactory.getLogger(MaturityProcessingBatch.class);

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired
    private BatchTimeService batchTimeService;

    /**
     * Run daily at 3:00 AM
     */
    @Scheduled(cron = "${batch.maturity-processing.cron:0 0 3 * * ?}")
    public void processMaturedAccounts() {
        LocalDate batchDate = batchTimeService.getBatchDate();
        logger.info("🕐 Starting Maturity Processing Batch for date: {}", batchDate);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        try {
            // Find all active accounts that matured on or before batch date
            List<FdAccount> maturedAccounts = accountRepository.findByStatusAndMaturityDateLessThanEqual(
                    FdAccount.AccountStatus.ACTIVE,
                    batchDate
            );
            
            logger.info("📊 Found {} accounts matured as of {}", maturedAccounts.size(), batchDate);

            for (FdAccount account : maturedAccounts) {
                try {
                    // Process each account in its own transaction
                    boolean processed = processAccountMaturity(account, batchDate);
                    if (processed) {
                        successCount++;
                    } else {
                        skipCount++;
                    }

                } catch (Exception e) {
                    errorCount++;
                    logger.error("❌ Error processing maturity for account {}: {}", 
                            account.getAccountNumber(), e.getMessage(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("✅ Maturity Processing Batch completed in {}ms - Success: {}, Skipped: {}, Errors: {}", 
                    duration, successCount, skipCount, errorCount);

        } catch (Exception e) {
            logger.error("❌ Maturity Processing Batch failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Process maturity for a single account (in its own transaction)
     */
    @Transactional
    private boolean processAccountMaturity(FdAccount account, LocalDate batchDate) {
        // Check if already processed (idempotency check)
        if (isAlreadyMatured(account)) {
            logger.info("⏭️ Skipping account {} - already has maturity transaction", 
                    account.getAccountNumber());
            return false;
        }

        // Process maturity
        processMaturity(account, batchDate);
        logger.info("✅ Processed maturity for account {}", account.getAccountNumber());
        return true;
    }

    /**
     * Check if account already has a maturity transaction (idempotency check)
     */
    private boolean isAlreadyMatured(FdAccount account) {
        // Check if a maturity credit transaction already exists
        List<FdTransaction> maturityTransactions = transactionRepository
                .findByAccountAndTransactionTypeAndTransactionDateBetween(
                        account,
                        FdTransaction.TransactionType.MATURITY_CREDIT,
                        account.getEffectiveDate().atStartOfDay(),
                        LocalDateTime.now().plusYears(10) // Far future date
                );
        
        boolean hasMaturityTransaction = !maturityTransactions.isEmpty();
        
        if (hasMaturityTransaction) {
            logger.info("🔒 Account {} already has maturity transaction", account.getAccountNumber());
        }
        
        return hasMaturityTransaction;
    }

    /**
     * Process maturity for an account
     */
    private void processMaturity(FdAccount account, LocalDate maturityDate) {
        // Calculate maturity amount (principal + all interest)
        BigDecimal principal = account.getPrincipalAmount();
        BigDecimal maturityAmount = account.getMaturityAmount();
        BigDecimal interestEarned = maturityAmount.subtract(principal);

        // Create maturity credit transaction
        FdTransaction transaction = FdTransaction.builder()
                .transactionId(generateTransactionId())
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(FdTransaction.TransactionType.MATURITY_CREDIT)
                .amount(maturityAmount)
                .balanceBefore(principal)
                .balanceAfter(maturityAmount)
                .status(FdTransaction.TransactionStatus.COMPLETED)
                .description("FD maturity processing")
                .remarks(String.format("BATCH: Maturity amount credited - Principal: %s, Interest: %s", 
                        principal, interestEarned))
                .initiatedBy("SYSTEM-BATCH")
                .approvedBy("SYSTEM-BATCH")
                .transactionDate(maturityDate.atTime(3, 0))
                .approvalDate(maturityDate.atTime(3, 0))
                .valueDate(maturityDate.atTime(3, 0))
                .channel("BATCH")
                .branchCode(account.getBranchCode())
                .build();

        transactionRepository.save(transaction);

        // Update account status to MATURED
        account.setStatus(FdAccount.AccountStatus.MATURED);
        account.setMaturityDate(maturityDate);
        accountRepository.save(account);
    }

    private String generateTransactionId() {
        return "TXN-" + LocalDateTime.now().toString().replaceAll("[:-]", "").substring(0, 17);
    }
}
