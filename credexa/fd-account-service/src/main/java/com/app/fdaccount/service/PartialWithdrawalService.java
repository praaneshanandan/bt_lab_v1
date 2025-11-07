package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.fdaccount.dto.PartialWithdrawalRequest;
import com.app.fdaccount.dto.PartialWithdrawalResponse;
import com.app.fdaccount.dto.TransactionRequest;
import com.app.fdaccount.dto.TransactionResponse;
import com.app.fdaccount.dto.external.ProductDto;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.enums.AccountStatus;
import com.app.fdaccount.enums.TransactionType;
import com.app.fdaccount.repository.FdAccountRepository;
import com.app.fdaccount.service.integration.ProductServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for partial withdrawal from FD accounts
 * Allows customers to withdraw part of their principal while maintaining minimum balance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartialWithdrawalService {

    private final FdAccountRepository accountRepository;
    private final ProductServiceClient productServiceClient;
    private final TransactionService transactionService;

    /**
     * Inquire if partial withdrawal is allowed and calculate remaining balance
     */
    @Transactional(readOnly = true)
    public PartialWithdrawalResponse inquirePartialWithdrawal(PartialWithdrawalRequest request) {
        log.info("Partial withdrawal inquiry for account: {}, amount: {}", 
                request.getAccountNumber(), request.getWithdrawalAmount());

        // 1. Find account
        FdAccount account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountNumber()));

        // 2. Validate account status
        if (account.getStatus() != AccountStatus.ACTIVE) {
            return buildIneligibleResponse(account, request.getWithdrawalAmount(), 
                    "Account is not active");
        }

        // 3. Check if withdrawal date is before maturity
        if (!request.getWithdrawalDate().isBefore(account.getMaturityDate())) {
            return buildIneligibleResponse(account, request.getWithdrawalAmount(), 
                    "Cannot perform partial withdrawal on or after maturity date");
        }

        // 4. Check if product allows partial withdrawal
        ProductDto product = productServiceClient.getProductByCode(account.getProductCode());
        if (!Boolean.TRUE.equals(product.getPartialWithdrawalAllowed())) {
            return buildIneligibleResponse(account, request.getWithdrawalAmount(), 
                    "Product does not allow partial withdrawal");
        }

        // 5. Get current principal balance
        BigDecimal currentBalance = account.getPrincipalAmount();

        // 6. Calculate balance after withdrawal
        BigDecimal balanceAfter = currentBalance.subtract(request.getWithdrawalAmount());

        // 7. Check minimum balance requirement
        BigDecimal minBalanceRequired = product.getMinBalanceRequired() != null ? 
                product.getMinBalanceRequired() : product.getMinAmount();

        if (balanceAfter.compareTo(minBalanceRequired) < 0) {
            return buildIneligibleResponse(account, request.getWithdrawalAmount(), 
                    String.format("Withdrawal would bring balance below minimum required (%.2f). Remaining balance would be %.2f", 
                            minBalanceRequired, balanceAfter));
        }

        // 8. Build successful response
        return PartialWithdrawalResponse.builder()
                .accountNumber(account.getAccountNumber())
                .withdrawalAmount(request.getWithdrawalAmount())
                .principalBalanceBefore(currentBalance)
                .principalBalanceAfter(balanceAfter)
                .minBalanceRequired(minBalanceRequired)
                .withdrawalDate(request.getWithdrawalDate())
                .isEligible(true)
                .message(String.format("Partial withdrawal of %.2f is allowed. Remaining balance will be %.2f", 
                        request.getWithdrawalAmount(), balanceAfter))
                .build();
    }

    /**
     * Process partial withdrawal
     */
    @Transactional
    public PartialWithdrawalResponse processPartialWithdrawal(PartialWithdrawalRequest request) {
        log.info("Processing partial withdrawal for account: {}, amount: {}", 
                request.getAccountNumber(), request.getWithdrawalAmount());

        // 1. Validate through inquiry first
        PartialWithdrawalResponse inquiry = inquirePartialWithdrawal(request);

        if (!inquiry.getIsEligible()) {
            throw new IllegalStateException("Partial withdrawal not allowed: " + inquiry.getMessage());
        }

        // 2. Find account
        FdAccount account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountNumber()));

        // 3. Create partial withdrawal transaction
        TransactionRequest txnRequest = TransactionRequest.builder()
                .accountNumber(request.getAccountNumber())
                .transactionType(TransactionType.PARTIAL_WITHDRAWAL)
                .amount(request.getWithdrawalAmount())
                .transactionDate(request.getWithdrawalDate())
                .valueDate(request.getWithdrawalDate())
                .description("Partial withdrawal from FD account - " + 
                        (request.getRemarks() != null ? request.getRemarks() : "Customer request"))
                .performedBy(request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM")
                .build();

        TransactionResponse txnResponse = transactionService.createTransaction(txnRequest);

        // 4. Update account principal amount
        account.setPrincipalAmount(inquiry.getPrincipalBalanceAfter());
        account.setUpdatedBy(request.getPerformedBy() != null ? request.getPerformedBy() : "SYSTEM");
        accountRepository.save(account);

        log.info("✅ Processed partial withdrawal for account: {}. New balance: {}", 
                request.getAccountNumber(), inquiry.getPrincipalBalanceAfter());

        // 5. Return response with transaction reference
        inquiry.setTransactionReference(txnResponse.getTransactionReference());
        return inquiry;
    }

    /**
     * Build ineligible response
     */
    private PartialWithdrawalResponse buildIneligibleResponse(
            FdAccount account, 
            BigDecimal withdrawalAmount, 
            String message) {
        
        return PartialWithdrawalResponse.builder()
                .accountNumber(account.getAccountNumber())
                .withdrawalAmount(withdrawalAmount)
                .principalBalanceBefore(account.getPrincipalAmount())
                .isEligible(false)
                .message(message)
                .build();
    }
}
