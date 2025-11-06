package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.fdaccount.entity.AccountBalance;
import com.app.fdaccount.entity.AccountTransaction;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.enums.AccountStatus;
import com.app.fdaccount.enums.TransactionType;
import com.app.fdaccount.repository.FdAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for manual maturity closure operations
 * Lab L19: Maturity Calculation, Closure, and Final Payout Execution
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaturityClosureService {

    private final FdAccountRepository accountRepository;
    private final NotificationService notificationService;

    /**
     * Manually close a matured FD account
     * Calculates final maturity amount and creates payout transaction
     * 
     * @param accountNumber FD account number
     * @param performedBy User initiating the closure
     * @return Closure response with maturity details
     */
    @Transactional
    public MaturityClosureResponse closeMaturedAccount(String accountNumber, String performedBy) {
        log.info("Manual maturity closure initiated for account: {} by {}", accountNumber, performedBy);

        // Fetch account
        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("FD Account not found: " + accountNumber));

        // Validate account is eligible for closure
        validateAccountForClosure(account);

        LocalDate today = LocalDate.now();

        // Re-validate maturity date
        if (today.isBefore(account.getMaturityDate())) {
            throw new IllegalStateException(
                    String.format("Account has not yet matured. Maturity date is %s",
                            account.getMaturityDate()));
        }

        // Calculate final maturity amount (Principal + Accrued Interest)
        BigDecimal currentPrincipal = getCurrentBalance(account, "PRINCIPAL");
        BigDecimal currentInterest = getCurrentBalance(account, "INTEREST_ACCRUED");
        BigDecimal maturityAmount = currentPrincipal.add(currentInterest);

        log.info("Maturity calculation for {}: Principal={}, Interest={}, Total={}",
                accountNumber, currentPrincipal, currentInterest, maturityAmount);

        // Create MATURITY_PAYOUT transaction
        AccountTransaction payoutTransaction = AccountTransaction.builder()
                .transactionReference(generateTransactionReference())
                .transactionType(TransactionType.MATURITY_PAYOUT)
                .amount(maturityAmount)
                .transactionDate(today)
                .valueDate(today)
                .description(String.format("Manual maturity payout - Final closure by %s", performedBy))
                .principalBalanceAfter(BigDecimal.ZERO)
                .interestBalanceAfter(BigDecimal.ZERO)
                .totalBalanceAfter(BigDecimal.ZERO)
                .performedBy(performedBy)
                .isReversed(false)
                .build();

        account.addTransaction(payoutTransaction);

        // Update account status to CLOSED
        account.setStatus(AccountStatus.MATURED);
        account.setClosureDate(today);
        account.setUpdatedBy(performedBy);

        // Update balances to zero
        updateBalance(account, "PRINCIPAL", BigDecimal.ZERO, today);
        updateBalance(account, "INTEREST_ACCRUED", BigDecimal.ZERO, today);
        updateBalance(account, "AVAILABLE", BigDecimal.ZERO, today);

        // Save account
        accountRepository.save(account);

        // Send notification to customer
        try {
            notificationService.sendMaturityPayoutNotification(account);
        } catch (Exception e) {
            log.error("Failed to send maturity notification for account: {}", accountNumber, e);
            // Don't fail the closure if notification fails
        }

        log.info("✅ Account {} successfully closed. Maturity amount {} paid out.",
                accountNumber, maturityAmount);

        return MaturityClosureResponse.builder()
                .accountNumber(accountNumber)
                .status("CLOSED")
                .maturityAmount(maturityAmount)
                .principalAmount(currentPrincipal)
                .interestAmount(currentInterest)
                .closureDate(today)
                .transactionReference(payoutTransaction.getTransactionReference())
                .message("FD account closed successfully. Maturity payout processed.")
                .build();
    }

    /**
     * Validate if account can be closed
     */
    private void validateAccountForClosure(FdAccount account) {
        if (account.getStatus() == AccountStatus.MATURED) {
            throw new IllegalStateException("Account is already closed/matured");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE accounts can be closed. Current status: " + account.getStatus());
        }
    }

    /**
     * Get current balance of a specific type
     */
    private BigDecimal getCurrentBalance(FdAccount account, String balanceType) {
        return account.getBalances().stream()
                .filter(b -> balanceType.equals(b.getBalanceType()))
                .max((b1, b2) -> b1.getAsOfDate().compareTo(b2.getAsOfDate()))
                .map(AccountBalance::getBalance)
                .orElse(balanceType.equals("PRINCIPAL") ? account.getPrincipalAmount() : BigDecimal.ZERO);
    }

    /**
     * Update balance
     */
    private void updateBalance(FdAccount account, String balanceType, BigDecimal balance, LocalDate date) {
        AccountBalance accountBalance = AccountBalance.builder()
                .balanceType(balanceType)
                .balance(balance)
                .asOfDate(date)
                .description("Balance after manual maturity closure")
                .build();

        account.addBalance(accountBalance);
    }

    /**
     * Generate transaction reference
     */
    private String generateTransactionReference() {
        return "TXN-" + LocalDate.now().toString().replace("-", "") + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Response DTO for maturity closure
     */
    @lombok.Data
    @lombok.Builder
    public static class MaturityClosureResponse {
        private String accountNumber;
        private String status;
        private BigDecimal maturityAmount;
        private BigDecimal principalAmount;
        private BigDecimal interestAmount;
        private LocalDate closureDate;
        private String transactionReference;
        private String message;
    }
}
