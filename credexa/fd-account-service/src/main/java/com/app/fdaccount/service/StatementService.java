package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.fdaccount.entity.AccountBalance;
import com.app.fdaccount.entity.AccountStatement;
import com.app.fdaccount.entity.AccountStatement.StatementType;
import com.app.fdaccount.entity.AccountTransaction;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.enums.TransactionType;
import com.app.fdaccount.repository.AccountStatementRepository;
import com.app.fdaccount.repository.FdAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating account statements
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {
    
    private final FdAccountRepository accountRepository;
    private final AccountStatementRepository statementRepository;
    
    /**
     * Generate daily statements for all active accounts
     */
    @Transactional
    public int generateDailyStatements() {
        log.info("Generating daily statements for all active accounts");
        
        LocalDate today = LocalDate.now();
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        
        int generated = 0;
        int skipped = 0;
        int errors = 0;
        
        for (FdAccount account : activeAccounts) {
            try {
                // Check if statement already exists
                if (statementRepository.existsByAccountIdAndDateAndType(
                        account.getId(), today, StatementType.DAILY)) {
                    log.debug("Statement already exists for account: {}", account.getAccountNumber());
                    skipped++;
                    continue;
                }
                
                // Generate statement
                AccountStatement statement = generateStatement(account, StatementType.DAILY, today, today);
                statementRepository.save(statement);
                
                log.debug("Generated daily statement for account: {}", account.getAccountNumber());
                generated++;
                
            } catch (Exception e) {
                log.error("Error generating statement for account: {}", account.getAccountNumber(), e);
                errors++;
            }
        }
        
        log.info("Daily statement generation completed - Generated: {}, Skipped: {}, Errors: {}", 
                generated, skipped, errors);
        
        return generated;
    }
    
    /**
     * Generate monthly statements for all active accounts
     */
    @Transactional
    public int generateMonthlyStatements() {
        log.info("Generating monthly statements for all active accounts");
        
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
        
        int generated = 0;
        int skipped = 0;
        int errors = 0;
        
        for (FdAccount account : activeAccounts) {
            try {
                // Check if monthly statement already exists
                if (statementRepository.existsByAccountIdAndDateAndType(
                        account.getId(), today, StatementType.MONTHLY)) {
                    log.debug("Monthly statement already exists for account: {}", account.getAccountNumber());
                    skipped++;
                    continue;
                }
                
                // Generate monthly statement
                AccountStatement statement = generateStatement(
                        account, StatementType.MONTHLY, firstDayOfMonth, lastDayOfMonth);
                statementRepository.save(statement);
                
                log.debug("Generated monthly statement for account: {}", account.getAccountNumber());
                generated++;
                
            } catch (Exception e) {
                log.error("Error generating monthly statement for account: {}", account.getAccountNumber(), e);
                errors++;
            }
        }
        
        log.info("Monthly statement generation completed - Generated: {}, Skipped: {}, Errors: {}", 
                generated, skipped, errors);
        
        return generated;
    }
    
    /**
     * Generate statement for a specific account and period
     */
    @Transactional
    public AccountStatement generateStatement(
            FdAccount account, 
            StatementType statementType,
            LocalDate periodStart,
            LocalDate periodEnd) {
        
        log.debug("Generating {} statement for account: {} (Period: {} to {})", 
                statementType, account.getAccountNumber(), periodStart, periodEnd);
        
        // Get opening balances (balances as of period start - 1 day)
        LocalDate openingDate = periodStart.minusDays(1);
        BigDecimal openingPrincipal = getBalanceAsOf(account, "PRINCIPAL", openingDate);
        BigDecimal openingInterest = getBalanceAsOf(account, "INTEREST_ACCRUED", openingDate);
        
        // Get closing balances (balances as of period end)
        BigDecimal closingPrincipal = getBalanceAsOf(account, "PRINCIPAL", periodEnd);
        BigDecimal closingInterest = getBalanceAsOf(account, "INTEREST_ACCRUED", periodEnd);
        
        // Get transactions for the period
        List<AccountTransaction> periodTransactions = account.getTransactions().stream()
                .filter(t -> !t.getTransactionDate().isBefore(periodStart) && 
                            !t.getTransactionDate().isAfter(periodEnd))
                .collect(Collectors.toList());
        
        // Calculate interest accrued and paid
        BigDecimal interestAccrued = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INTEREST_ACCRUAL)
                .map(AccountTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal interestPaid = periodTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INTEREST_CREDIT ||
                            t.getTransactionType() == TransactionType.MATURITY_PAYOUT)
                .map(AccountTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calculate total credits and debits
        BigDecimal totalCredits = periodTransactions.stream()
                .filter(t -> isCredit(t.getTransactionType()))
                .map(AccountTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDebits = periodTransactions.stream()
                .filter(t -> isDebit(t.getTransactionType()))
                .map(AccountTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Total closing balance
        BigDecimal totalClosing = closingPrincipal.add(closingInterest);
        
        // Build summary
        String summary = buildSummary(account, periodStart, periodEnd, periodTransactions.size(),
                interestAccrued, interestPaid, totalCredits, totalDebits);
        
        // Create statement
        return AccountStatement.builder()
                .account(account)
                .statementReference(generateStatementReference(account, statementType))
                .statementType(statementType)
                .statementDate(periodEnd)
                .periodStartDate(periodStart)
                .periodEndDate(periodEnd)
                .openingPrincipalBalance(openingPrincipal)
                .closingPrincipalBalance(closingPrincipal)
                .openingInterestBalance(openingInterest)
                .closingInterestBalance(closingInterest)
                .interestAccrued(interestAccrued)
                .interestPaid(interestPaid)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .transactionCount(periodTransactions.size())
                .totalClosingBalance(totalClosing)
                .summary(summary)
                .generatedBy("SYSTEM-BATCH")
                .build();
    }
    
    /**
     * Get balance as of a specific date
     */
    private BigDecimal getBalanceAsOf(FdAccount account, String balanceType, LocalDate asOfDate) {
        return account.getBalances().stream()
                .filter(b -> balanceType.equals(b.getBalanceType()) && 
                            !b.getAsOfDate().isAfter(asOfDate))
                .max((b1, b2) -> b1.getAsOfDate().compareTo(b2.getAsOfDate()))
                .map(AccountBalance::getBalance)
                .orElse(balanceType.equals("PRINCIPAL") ? account.getPrincipalAmount() : BigDecimal.ZERO);
    }
    
    /**
     * Check if transaction type is a credit
     */
    private boolean isCredit(TransactionType type) {
        return type == TransactionType.INITIAL_DEPOSIT ||
               type == TransactionType.ADDITIONAL_DEPOSIT ||
               type == TransactionType.INTEREST_ACCRUAL ||
               type == TransactionType.INTEREST_CREDIT ||
               type == TransactionType.INTEREST_CAPITALIZATION;
    }
    
    /**
     * Check if transaction type is a debit
     */
    private boolean isDebit(TransactionType type) {
        return type == TransactionType.WITHDRAWAL ||
               type == TransactionType.PREMATURE_WITHDRAWAL ||
               type == TransactionType.FEE_DEBIT ||
               type == TransactionType.PENALTY ||
               type == TransactionType.MATURITY_PAYOUT;
    }
    
    /**
     * Build statement summary
     */
    private String buildSummary(FdAccount account, LocalDate periodStart, LocalDate periodEnd,
                                int transactionCount, BigDecimal interestAccrued, 
                                BigDecimal interestPaid, BigDecimal totalCredits, 
                                BigDecimal totalDebits) {
        return String.format(
                "Account Statement for %s (%s)\n" +
                "Period: %s to %s\n" +
                "Product: %s\n" +
                "Interest Rate: %.2f%%\n" +
                "Transactions: %d\n" +
                "Interest Accrued: %.2f\n" +
                "Interest Paid: %.2f\n" +
                "Total Credits: %.2f\n" +
                "Total Debits: %.2f\n" +
                "Maturity Date: %s",
                account.getAccountNumber(),
                account.getAccountName(),
                periodStart,
                periodEnd,
                account.getProductName(),
                account.getInterestRate(),
                transactionCount,
                interestAccrued,
                interestPaid,
                totalCredits,
                totalDebits,
                account.getMaturityDate()
        );
    }
    
    /**
     * Generate unique statement reference
     */
    private String generateStatementReference(FdAccount account, StatementType type) {
        return String.format("STMT-%s-%s-%s-%s",
                type.name(),
                account.getAccountNumber(),
                LocalDate.now().toString().replace("-", ""),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }
    
    /**
     * Get statements for an account
     */
    public List<AccountStatement> getAccountStatements(String accountNumber) {
        return statementRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Get statement by reference
     */
    public AccountStatement getStatementByReference(String reference) {
        return statementRepository.findByStatementReference(reference)
                .orElseThrow(() -> new RuntimeException("Statement not found: " + reference));
    }
}
