package com.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.account.dto.InterestCalculationRequest;
import com.app.account.dto.InterestCalculationResponse;
import com.app.account.entity.FdAccount;
import com.app.account.entity.FdAccount.AccountStatus;
import com.app.account.entity.FdTransaction;
import com.app.account.entity.FdTransaction.TransactionStatus;
import com.app.account.entity.FdTransaction.TransactionType;
import com.app.account.repository.FdAccountRepository;
import com.app.account.repository.FdTransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service layer for Interest Calculation
 * Handles interest calculation and crediting for FD accounts
 */
@Service
public class InterestCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(InterestCalculationService.class);
    private static final AtomicLong transactionCounter = new AtomicLong(6000);
    private static final int DAYS_IN_YEAR = 365;

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * Calculate and optionally credit interest for an FD account
     */
    @Transactional
    public InterestCalculationResponse calculateInterest(InterestCalculationRequest request, String currentUser) {
        logger.info("💰 Processing interest calculation: Account={}, CreditInterest={}, ApplyTDS={}", 
                request.getAccountNumber(), 
                request.getCreditInterestOrDefault(), 
                request.getApplyTdsOrDefault());

        // 1. Find account
        FdAccount account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountNumber()));

        // 2. Validate account is active
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot calculate interest for closed account");
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot calculate interest for suspended account");
        }

        // 3. Determine calculation period
        LocalDate fromDate = determineFromDate(account, request.getFromDate());
        LocalDate toDate = request.getToDate() != null ? request.getToDate() : LocalDate.now();

        // Validate date range
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        if (fromDate.isBefore(account.getEffectiveDate())) {
            throw new IllegalArgumentException("From date cannot be before account effective date");
        }

        if (toDate.isAfter(account.getMaturityDate())) {
            toDate = account.getMaturityDate();
            logger.info("⚠️ To date adjusted to maturity date: {}", toDate);
        }

        long daysInPeriod = ChronoUnit.DAYS.between(fromDate, toDate);

        if (daysInPeriod <= 0) {
            throw new IllegalArgumentException("No days to calculate interest. From date: " + fromDate + ", To date: " + toDate);
        }

        logger.info("📅 Calculation period: {} to {} ({} days)", fromDate, toDate, daysInPeriod);

        // 4. Calculate interest
        BigDecimal principal = account.getPrincipalAmount();
        BigDecimal annualRate = account.getInterestRate();

        // Simple interest formula: (P × R × T) / (100 × 365)
        BigDecimal interestAmount = principal
                .multiply(annualRate)
                .multiply(BigDecimal.valueOf(daysInPeriod))
                .divide(BigDecimal.valueOf(100 * DAYS_IN_YEAR), 2, RoundingMode.HALF_UP);

        logger.info("💵 Interest calculated: Principal={}, Rate={}, Days={}, Interest={}", 
                principal, annualRate, daysInPeriod, interestAmount);

        // 5. Calculate TDS (if applicable)
        BigDecimal tdsAmount = BigDecimal.ZERO;
        BigDecimal netInterest = interestAmount;
        boolean tdsApplicable = account.getTdsApplicable() != null && account.getTdsApplicable();

        if (tdsApplicable && request.getApplyTdsOrDefault()) {
            BigDecimal tdsRate = account.getTdsRate() != null ? account.getTdsRate() : BigDecimal.ZERO;
            tdsAmount = interestAmount
                    .multiply(tdsRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            netInterest = interestAmount.subtract(tdsAmount);
            logger.info("💳 TDS calculated: Rate={}, Amount={}, Net Interest={}", tdsRate, tdsAmount, netInterest);
        }

        // 6. Get current balance
        BigDecimal balanceBefore = getCurrentBalance(account);

        // 7. Get previous interest/TDS summary
        BigDecimal totalInterestCredited = transactionRepository
                .findByAccountNumberAndTransactionType(account.getAccountNumber(), TransactionType.INTEREST_CREDIT, Pageable.unpaged())
                .stream()
                .map(FdTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTdsDeducted = transactionRepository
                .findByAccountNumberAndTransactionType(account.getAccountNumber(), TransactionType.TDS_DEDUCTION, Pageable.unpaged())
                .stream()
                .map(FdTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long previousInterestCredits = transactionRepository
                .countByAccountNumberAndTransactionType(account.getAccountNumber(), TransactionType.INTEREST_CREDIT);

        // 8. Credit interest and deduct TDS (if requested)
        String interestTransactionId = null;
        String tdsTransactionId = null;
        BigDecimal balanceAfter = balanceBefore;
        List<String> transactionsCreated = new ArrayList<>();

        if (request.getCreditInterestOrDefault()) {
            // Create interest credit transaction
            balanceAfter = balanceAfter.add(interestAmount);
            interestTransactionId = createInterestTransaction(
                    account, 
                    interestAmount, 
                    balanceBefore, 
                    balanceAfter,
                    fromDate,
                    toDate,
                    request.getPaymentReference(),
                    request.getRemarks(),
                    currentUser
            );
            transactionsCreated.add("INTEREST_CREDIT: " + interestTransactionId);
            logger.info("✅ Interest credited: Transaction={}, Amount={}", interestTransactionId, interestAmount);

            // Create TDS deduction transaction (if applicable)
            if (tdsApplicable && request.getApplyTdsOrDefault() && tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal balanceBeforeTds = balanceAfter;
                balanceAfter = balanceAfter.subtract(tdsAmount);
                tdsTransactionId = createTdsTransaction(
                        account,
                        tdsAmount,
                        balanceBeforeTds,
                        balanceAfter,
                        fromDate,
                        toDate,
                        request.getPaymentReference(),
                        currentUser
                );
                transactionsCreated.add("TDS_DEDUCTION: " + tdsTransactionId);
                logger.info("✅ TDS deducted: Transaction={}, Amount={}", tdsTransactionId, tdsAmount);
            }
        }

        // 9. Build calculation breakdown
        InterestCalculationResponse.CalculationBreakdown breakdown = InterestCalculationResponse.CalculationBreakdown.builder()
                .principal(principal)
                .annualRate(annualRate)
                .days(daysInPeriod)
                .daysInYear(DAYS_IN_YEAR)
                .grossInterest(interestAmount)
                .tdsApplicable(tdsApplicable && request.getApplyTdsOrDefault())
                .tdsRate(account.getTdsRate())
                .tdsAmount(tdsAmount)
                .netInterest(netInterest)
                .formula("Simple Interest: (Principal × Rate × Days) / (100 × 365)")
                .transactionsCreated(transactionsCreated)
                .build();

        // 10. Build response
        String message = String.format("Interest calculated: ₹%,.2f for %d days (%s to %s)", 
                interestAmount, daysInPeriod, fromDate, toDate);
        
        if (request.getCreditInterestOrDefault()) {
            message += String.format(". Net credited: ₹%,.2f", netInterest);
        }

        InterestCalculationResponse response = InterestCalculationResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .accountStatus(account.getStatus().name())
                .fromDate(fromDate)
                .toDate(toDate)
                .daysInPeriod(daysInPeriod)
                .principalAmount(principal)
                .interestRate(annualRate)
                .calculationType(account.getCalculationType())
                .compoundingFrequency(account.getCompoundingFrequency())
                .interestAmount(interestAmount)
                .tdsRate(account.getTdsRate())
                .tdsAmount(tdsAmount)
                .netInterest(netInterest)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .interestCredited(request.getCreditInterestOrDefault())
                .tdsDeducted(request.getApplyTdsOrDefault() && tdsAmount.compareTo(BigDecimal.ZERO) > 0)
                .interestTransactionId(interestTransactionId)
                .tdsTransactionId(tdsTransactionId)
                .breakdown(breakdown)
                .totalInterestCreditedTillDate(totalInterestCredited.add(request.getCreditInterestOrDefault() ? interestAmount : BigDecimal.ZERO))
                .totalTdsDeductedTillDate(totalTdsDeducted.add(request.getApplyTdsOrDefault() ? tdsAmount : BigDecimal.ZERO))
                .previousInterestCreditsCount(previousInterestCredits)
                .message(message)
                .remarks(request.getRemarks())
                .build();

        logger.info("✅ Interest calculation completed: Gross={}, TDS={}, Net={}, Credited={}", 
                interestAmount, tdsAmount, netInterest, request.getCreditInterestOrDefault());

        return response;
    }

    /**
     * Determine from date for calculation
     * If not provided, use last interest credit date or effective date
     */
    private LocalDate determineFromDate(FdAccount account, LocalDate requestedFromDate) {
        if (requestedFromDate != null) {
            return requestedFromDate;
        }

        // Get last interest credit transaction
        return transactionRepository
                .findFirstByAccountNumberAndTransactionTypeOrderByTransactionDateDesc(
                        account.getAccountNumber(), 
                        TransactionType.INTEREST_CREDIT
                )
                .map(tx -> tx.getTransactionDate().toLocalDate().plusDays(1)) // Next day after last credit
                .orElse(account.getEffectiveDate()); // Or account effective date
    }

    /**
     * Create interest credit transaction
     */
    private String createInterestTransaction(FdAccount account, BigDecimal amount, 
                                            BigDecimal balanceBefore, BigDecimal balanceAfter,
                                            LocalDate fromDate, LocalDate toDate,
                                            String paymentReference, String remarks,
                                            String currentUser) {
        String transactionId = generateTransactionId();
        String description = String.format("Interest for period %s to %s (%d days)", 
                fromDate, toDate, ChronoUnit.DAYS.between(fromDate, toDate));

        FdTransaction transaction = FdTransaction.builder()
                .transactionId(transactionId)
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(TransactionType.INTEREST_CREDIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.COMPLETED)
                .referenceNumber(paymentReference != null ? paymentReference : "INT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .description(description)
                .remarks(remarks)
                .initiatedBy(currentUser)
                .approvedBy(currentUser)
                .transactionDate(LocalDateTime.now())
                .approvalDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .channel("SYSTEM")
                .branchCode(account.getBranchCode())
                .ipAddress(getClientIpAddress())
                .build();

        transactionRepository.save(transaction);
        return transactionId;
    }

    /**
     * Create TDS deduction transaction
     */
    private String createTdsTransaction(FdAccount account, BigDecimal amount,
                                       BigDecimal balanceBefore, BigDecimal balanceAfter,
                                       LocalDate fromDate, LocalDate toDate,
                                       String paymentReference, String currentUser) {
        String transactionId = generateTransactionId();
        String description = String.format("TDS on interest for period %s to %s", fromDate, toDate);

        FdTransaction transaction = FdTransaction.builder()
                .transactionId(transactionId)
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(TransactionType.TDS_DEDUCTION)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.COMPLETED)
                .referenceNumber(paymentReference != null ? paymentReference : "TDS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .description(description)
                .remarks("Tax Deducted at Source")
                .initiatedBy(currentUser)
                .approvedBy(currentUser)
                .transactionDate(LocalDateTime.now())
                .approvalDate(LocalDateTime.now())
                .valueDate(LocalDateTime.now())
                .channel("SYSTEM")
                .branchCode(account.getBranchCode())
                .ipAddress(getClientIpAddress())
                .build();

        transactionRepository.save(transaction);
        return transactionId;
    }

    /**
     * Get current balance from latest transaction or principal
     */
    private BigDecimal getCurrentBalance(FdAccount account) {
        return transactionRepository.findFirstByAccountNumberOrderByTransactionDateDesc(account.getAccountNumber())
                .map(FdTransaction::getBalanceAfter)
                .orElse(account.getPrincipalAmount());
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        long counter = transactionCounter.incrementAndGet();
        return "TXN-" + timestamp + "-" + String.format("%04d", counter);
    }

    /**
     * Get client IP address
     */
    private String getClientIpAddress() {
        if (httpServletRequest == null) {
            return "SYSTEM";
        }

        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String remoteAddr = httpServletRequest.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "SYSTEM";
    }
}
