package com.app.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.account.dto.AccountInquiryRequest.AccountIdType;
import com.app.account.dto.RedemptionInquiryRequest;
import com.app.account.dto.RedemptionInquiryResponse;
import com.app.account.dto.RedemptionProcessRequest;
import com.app.account.dto.RedemptionProcessResponse;
import com.app.account.entity.FdAccount;
import com.app.account.entity.FdAccount.AccountStatus;
import com.app.account.entity.FdTransaction;
import com.app.account.entity.FdTransaction.TransactionStatus;
import com.app.account.entity.FdTransaction.TransactionType;
import com.app.account.repository.FdAccountRepository;
import com.app.account.repository.FdTransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service layer for FD Redemption operations
 * Handles redemption inquiry and processing
 */
@Service
public class RedemptionService {

    private static final Logger logger = LoggerFactory.getLogger(RedemptionService.class);
    private static final AtomicLong transactionCounter = new AtomicLong(5000);

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * Get redemption inquiry details for an account
     */
    @Transactional(readOnly = true)
    public RedemptionInquiryResponse getRedemptionInquiry(RedemptionInquiryRequest request) {
        logger.info("🔍 Getting redemption inquiry: ID Type={}, ID Value={}", 
                request.getIdTypeOrDefault(), request.getIdValue());

        // 1. Find account by ID type
        FdAccount account = findAccountByIdType(request.getIdTypeOrDefault(), request.getIdValue());

        // 2. Validate account is not already closed
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed. Cannot perform redemption inquiry.");
        }

        // 3. Get transaction summary
        Long totalTransactions = transactionRepository.countByAccountNumber(account.getAccountNumber());
        Long interestCredits = transactionRepository.countByAccountNumberAndTransactionType(
                account.getAccountNumber(), TransactionType.INTEREST_CREDIT);
        Long tdsDeductions = transactionRepository.countByAccountNumberAndTransactionType(
                account.getAccountNumber(), TransactionType.TDS_DEDUCTION);

        // 4. Calculate current balance
        BigDecimal currentBalance = getCurrentBalance(account);

        // 5. Calculate interest earned (sum of all interest credits)
        BigDecimal interestEarned = transactionRepository.findByAccountNumberAndTransactionType(
                account.getAccountNumber(), TransactionType.INTEREST_CREDIT, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(FdTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Calculate TDS deducted (sum of all TDS deductions)
        BigDecimal tdsDeducted = transactionRepository.findByAccountNumberAndTransactionType(
                account.getAccountNumber(), TransactionType.TDS_DEDUCTION, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(FdTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. Calculate dates and tenure
        LocalDate today = LocalDate.now();
        long daysElapsed = ChronoUnit.DAYS.between(account.getEffectiveDate(), today);
        long daysRemaining = ChronoUnit.DAYS.between(today, account.getMaturityDate());
        int monthsElapsed = (int) ChronoUnit.MONTHS.between(account.getEffectiveDate(), today);
        int monthsRemaining = account.getTermMonths() - monthsElapsed;
        boolean isMatured = !today.isBefore(account.getMaturityDate());

        // 8. Determine redemption type
        String redemptionType;
        if (today.isBefore(account.getMaturityDate())) {
            redemptionType = "PREMATURE";
        } else if (today.isEqual(account.getMaturityDate())) {
            redemptionType = "ON_MATURITY";
        } else {
            redemptionType = "POST_MATURITY";
        }

        // 9. Calculate penalty (if premature)
        BigDecimal penaltyAmount = BigDecimal.ZERO;
        BigDecimal penaltyRate = BigDecimal.ZERO;
        boolean penaltyApplicable = false;
        String penaltyDescription = null;

        if ("PREMATURE".equals(redemptionType)) {
            penaltyApplicable = true;
            penaltyRate = new BigDecimal("0.50"); // 0.5% penalty on interest
            penaltyAmount = interestEarned.multiply(penaltyRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            penaltyDescription = "Premature redemption penalty: " + penaltyRate + "% on interest earned";
        }

        // 10. Calculate net redemption amount
        BigDecimal netRedemptionAmount = currentBalance
                .add(interestEarned)
                .subtract(tdsDeducted)
                .subtract(penaltyAmount);

        // 11. Build response
        RedemptionInquiryResponse response = RedemptionInquiryResponse.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .ibanNumber(account.getIbanNumber())
                .accountName(account.getAccountName())
                .accountStatus(account.getStatus().name())
                .customerId(account.getCustomerId())
                .customerName(account.getCustomerName())
                .productCode(account.getProductCode())
                .productName(account.getProductName())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(account.getInterestRate())
                .termMonths(account.getTermMonths())
                .maturityAmount(account.getMaturityAmount())
                .effectiveDate(account.getEffectiveDate())
                .maturityDate(account.getMaturityDate())
                .inquiryDate(today)
                .daysElapsed(daysElapsed)
                .daysRemaining(daysRemaining > 0 ? daysRemaining : 0)
                .monthsElapsed(monthsElapsed)
                .monthsRemaining(monthsRemaining > 0 ? monthsRemaining : 0)
                .isMatured(isMatured)
                .currentBalance(currentBalance)
                .interestEarned(interestEarned)
                .tdsDeducted(tdsDeducted)
                .penaltyAmount(penaltyAmount)
                .netRedemptionAmount(netRedemptionAmount)
                .penaltyApplicable(penaltyApplicable)
                .penaltyRate(penaltyRate)
                .penaltyDescription(penaltyDescription)
                .tdsRate(account.getTdsRate())
                .tdsApplicable(account.getTdsApplicable())
                .totalTransactions(totalTransactions)
                .interestCreditCount(interestCredits)
                .tdsDeductionCount(tdsDeductions)
                .redemptionType(redemptionType)
                .remarks(penaltyApplicable ? "Premature redemption - penalty applicable" : 
                        "Redemption on or after maturity - no penalty")
                .branchCode(account.getBranchCode())
                .branchName(account.getBranchName())
                .build();

        logger.info("✅ Redemption inquiry completed: Account={}, Net Amount={}, Type={}", 
                account.getAccountNumber(), netRedemptionAmount, redemptionType);

        return response;
    }

    /**
     * Process redemption (full or partial)
     */
    @Transactional
    public RedemptionProcessResponse processRedemption(RedemptionProcessRequest request, String currentUser) {
        logger.info("💰 Processing redemption: ID Type={}, ID Value={}, Type={}", 
                request.getIdTypeOrDefault(), request.getIdValue(), request.getRedemptionType());

        // 1. Find account
        FdAccount account = findAccountByIdType(request.getIdTypeOrDefault(), request.getIdValue());

        // 2. Validate account status
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed. Cannot process redemption.");
        }

        if (account.getStatus() == AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Account is suspended. Cannot process redemption.");
        }

        // 3. Validate redemption amount for PARTIAL
        if (request.getRedemptionType() == RedemptionProcessRequest.RedemptionTypeEnum.PARTIAL) {
            if (request.getRedemptionAmount() == null || request.getRedemptionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Redemption amount is required for partial redemption");
            }
        }

        // 4. Get redemption inquiry details
        RedemptionInquiryRequest inquiryRequest = RedemptionInquiryRequest.builder()
                .idType(request.getIdTypeOrDefault())
                .idValue(request.getIdValue())
                .build();
        RedemptionInquiryResponse inquiry = getRedemptionInquiry(inquiryRequest);

        // 5. Get current balance
        BigDecimal balanceBefore = getCurrentBalance(account);

        // 6. Calculate redemption amounts
        BigDecimal redemptionAmount;
        BigDecimal balanceAfter;
        AccountStatus newStatus;

        if (request.getRedemptionType() == RedemptionProcessRequest.RedemptionTypeEnum.FULL) {
            // Full redemption - close account
            redemptionAmount = inquiry.getNetRedemptionAmount();
            balanceAfter = BigDecimal.ZERO;
            newStatus = AccountStatus.CLOSED;
        } else {
            // Partial redemption - withdraw partial amount
            redemptionAmount = request.getRedemptionAmount();
            
            // Check if redemption amount exceeds available balance
            if (redemptionAmount.compareTo(inquiry.getNetRedemptionAmount()) > 0) {
                throw new IllegalArgumentException("Redemption amount exceeds available balance. " +
                        "Max available: " + inquiry.getNetRedemptionAmount());
            }

            // Check minimum balance requirement (10% of principal)
            BigDecimal minimumBalance = account.getPrincipalAmount().multiply(new BigDecimal("0.10"));
            balanceAfter = inquiry.getNetRedemptionAmount().subtract(redemptionAmount);
            
            if (balanceAfter.compareTo(minimumBalance) < 0) {
                throw new IllegalArgumentException("Remaining balance after redemption would be below minimum required balance. " +
                        "Minimum: " + minimumBalance + ", Remaining would be: " + balanceAfter);
            }

            newStatus = AccountStatus.ACTIVE; // Keep account active
        }

        // 7. Generate transaction ID
        String transactionId = generateTransactionId();

        // 8. Get IP address
        String ipAddress = getClientIpAddress();

        // 9. Determine transaction type
        TransactionType transactionType = request.getRedemptionType() == RedemptionProcessRequest.RedemptionTypeEnum.FULL
                ? TransactionType.CLOSURE
                : TransactionType.WITHDRAWAL;

        // 10. Create redemption transaction
        LocalDateTime redemptionDateTime = request.getRedemptionDate() != null 
                ? request.getRedemptionDate() 
                : LocalDateTime.now();

        FdTransaction transaction = FdTransaction.builder()
                .transactionId(transactionId)
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(transactionType)
                .amount(redemptionAmount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.COMPLETED)
                .referenceNumber(request.getPaymentReference())
                .description(request.getRedemptionType() + " redemption - " + 
                        (inquiry.getPenaltyApplicable() ? "Premature (penalty applied)" : "On/After maturity"))
                .remarks(request.getRemarks())
                .initiatedBy(currentUser)
                .approvedBy(currentUser)
                .transactionDate(redemptionDateTime)
                .approvalDate(redemptionDateTime)
                .valueDate(redemptionDateTime)
                .channel(request.getChannel() != null ? request.getChannel() : "API")
                .branchCode(request.getBranchCode() != null ? request.getBranchCode() : account.getBranchCode())
                .ipAddress(ipAddress)
                .build();

        FdTransaction savedTransaction = transactionRepository.save(transaction);

        // 11. Update account status
        account.setStatus(newStatus);
        if (newStatus == AccountStatus.CLOSED) {
            account.setClosureDate(redemptionDateTime.toLocalDate());
        }
        account.setUpdatedBy(currentUser);
        accountRepository.save(account);

        // 12. Build calculation breakdown
        RedemptionProcessResponse.CalculationBreakdown breakdown = RedemptionProcessResponse.CalculationBreakdown.builder()
                .balanceBefore(balanceBefore)
                .interestAmount(inquiry.getInterestEarned())
                .tdsAmount(inquiry.getTdsDeducted())
                .penaltyAmount(inquiry.getPenaltyAmount())
                .netAmount(inquiry.getNetRedemptionAmount())
                .penaltyApplicable(inquiry.getPenaltyApplicable())
                .penaltyReason(inquiry.getPenaltyDescription())
                .build();

        // 13. Build response
        RedemptionProcessResponse response = RedemptionProcessResponse.builder()
                .redemptionTransactionId(savedTransaction.getTransactionId())
                .redemptionStatus(savedTransaction.getStatus().name())
                .redemptionType(request.getRedemptionType().name())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .accountStatus(newStatus.name())
                .principalAmount(account.getPrincipalAmount())
                .interestEarned(inquiry.getInterestEarned())
                .tdsDeducted(inquiry.getTdsDeducted())
                .penaltyAmount(inquiry.getPenaltyAmount())
                .grossRedemptionAmount(balanceBefore.add(inquiry.getInterestEarned()))
                .netRedemptionAmount(redemptionAmount)
                .balanceAfter(balanceAfter)
                .paymentReference(request.getPaymentReference())
                .redemptionDate(redemptionDateTime)
                .processedBy(currentUser)
                .channel(savedTransaction.getChannel())
                .branchCode(savedTransaction.getBranchCode())
                .breakdown(breakdown)
                .remarks(request.getRemarks())
                .message("Redemption processed successfully. Net amount: ₹" + 
                        String.format("%,.2f", redemptionAmount))
                .build();

        logger.info("✅ Redemption processed: Transaction={}, Type={}, Amount={}, Account Status={}", 
                savedTransaction.getTransactionId(), 
                request.getRedemptionType(),
                redemptionAmount,
                newStatus);

        return response;
    }

    /**
     * Find account by ID type
     */
    private FdAccount findAccountByIdType(AccountIdType idType, String idValue) {
        return switch (idType) {
            case IBAN -> accountRepository.findByIbanNumber(idValue)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found with IBAN: " + idValue));
            case INTERNAL_ID -> accountRepository.findById(Long.parseLong(idValue))
                    .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + idValue));
            case ACCOUNT_NUMBER -> accountRepository.findByAccountNumber(idValue)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found with account number: " + idValue));
        };
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
        return remoteAddr != null ? remoteAddr : "UNKNOWN";
    }
}
