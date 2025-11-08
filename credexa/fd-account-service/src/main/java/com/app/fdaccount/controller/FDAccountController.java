package com.app.fdaccount.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.fdaccount.dto.AccountResponse;
import com.app.fdaccount.dto.CreateAccountRequest;
import com.app.fdaccount.dto.PrematureWithdrawalInquiryRequest;
import com.app.fdaccount.dto.PrematureWithdrawalInquiryResponse;
import com.app.fdaccount.dto.TransactionResponse;
import com.app.fdaccount.dto.WithdrawalRequest;
import com.app.fdaccount.dto.WithdrawalResponse;
import com.app.fdaccount.service.AccountCreationService;
import com.app.fdaccount.service.PrematureWithdrawalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * FD Account Controller for Lab L13
 * Provides the specific endpoint structure as per Lab L13 requirements
 * Delegates to the main AccountCreationService
 */
@Slf4j
@RestController
@RequestMapping("/fd/account")
@RequiredArgsConstructor
@Tag(name = "FD Account Operations (Labs L13 & L15)", description = "Fixed Deposit Account Creation and Withdrawal - Lab Specific Endpoints")
public class FDAccountController {

    private final AccountCreationService accountCreationService;
    private final PrematureWithdrawalService prematureWithdrawalService;

    /**
     * Lab L13: Create Fixed Deposit Account
     * This endpoint matches the exact specification from Lab L13 document
     * 
     * @param request Account creation request with product code, principal amount, term, etc.
     * @param auth Authentication object (Spring Security injects this automatically)
     * @return Response with FD account number and status
     */
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @PostMapping("/create")
    @Operation(
        summary = "Create FD Account (Lab L13)",
        description = "Creates a new Fixed Deposit account with automatic account number generation, " +
                     "product validation, rate determination, and customer linking. " +
                     "Requires MANAGER or ADMIN role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> createFDAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication auth) {
        
        log.info("LAB L13: Creating FD account for product: {} by user: {}", 
                request.getProductCode(), 
                auth != null ? auth.getName() : "SYSTEM");
        
        try {
            // Set created by from authentication if not provided
            if (request.getCreatedBy() == null && auth != null) {
                request.setCreatedBy(auth.getName());
            }
            
            // Create the account using the main service
            AccountResponse response = accountCreationService.createAccount(request);
            
            log.info("✅ LAB L13: FD Account created successfully: {}", response.getAccountNumber());
            
            // Return response in Lab L13 format
            return ResponseEntity.ok(Map.of(
                "fdAccountNo", response.getAccountNumber(),
                "status", "Account Created",
                "accountDetails", response
            ));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ LAB L13: Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "status", "Error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ LAB L13: Failed to create FD account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "Error",
                "message", "Failed to create FD account: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Lab L15: Premature Withdrawal from Fixed Deposit Account
     * This endpoint matches the exact specification from Lab L15 document
     * 
     * @param request Withdrawal request with FD account number, withdrawal date, and transfer account
     * @param auth Authentication object (Spring Security injects this automatically)
     * @return Response with withdrawal amount and penalty applied
     */
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('MANAGER')")
    @PostMapping("/withdraw")
    @Operation(
        summary = "Withdraw FD Account (Lab L15)",
        description = "Process premature withdrawal from a Fixed Deposit account. " +
                     "Calculates interest accrued till withdrawal date, applies penalty, " +
                     "updates account status to CLOSED, and generates withdrawal transaction. " +
                     "Requires CUSTOMER (own account) or MANAGER role.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> withdrawFD(
            @Valid @RequestBody WithdrawalRequest request,
            Authentication auth) {
        
        String username = auth != null ? auth.getName() : "SYSTEM";
        log.info("LAB L15: Processing FD withdrawal for account: {} by user: {}", 
                request.getFdAccountNo(), username);
        
        try {
            // Step 1: Get withdrawal inquiry details (calculate penalty, net amount)
            PrematureWithdrawalInquiryRequest inquiryRequest = PrematureWithdrawalInquiryRequest.builder()
                    .accountNumber(request.getFdAccountNo())
                    .withdrawalDate(request.getWithdrawalDate())
                    .build();
            
            PrematureWithdrawalInquiryResponse inquiry = 
                    prematureWithdrawalService.inquirePrematureWithdrawal(inquiryRequest);
            
            // Step 2: Check eligibility
            if (!inquiry.getIsEligible()) {
                log.warn("❌ LAB L15: Account not eligible for withdrawal: {}", inquiry.getMessage());
                return ResponseEntity.badRequest().body(WithdrawalResponse.builder()
                        .status("failure")
                        .message(inquiry.getMessage())
                        .fdAccountNo(request.getFdAccountNo())
                        .build());
            }
            
            // Step 3: Process the withdrawal
            TransactionResponse txnResponse = prematureWithdrawalService.processPrematureWithdrawal(
                    request.getFdAccountNo(),
                    request.getWithdrawalDate(),
                    username,
                    "Lab L15 - Premature withdrawal via API"
            );
            
            // Step 4: Build Lab L15 response format
            WithdrawalResponse response = WithdrawalResponse.builder()
                    .status("success")
                    .message("FD account closed successfully.")
                    .fdAccountNo(request.getFdAccountNo())
                    .withdrawalAmount(inquiry.getNetPayable())
                    .penaltyApplied(inquiry.getPenaltyAmount())
                    .principalAmount(inquiry.getPrincipalAmount())
                    .interestEarned(inquiry.getInterestEarned())
                    .tdsDeducted(inquiry.getTdsAmount())
                    .transactionReference(txnResponse.getTransactionReference())
                    .build();
            
            log.info("✅ LAB L15: FD withdrawal successful - Account: {}, Amount: {}, Penalty: {}", 
                    request.getFdAccountNo(), 
                    inquiry.getNetPayable(), 
                    inquiry.getPenaltyAmount());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("❌ LAB L15: Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(WithdrawalResponse.builder()
                    .status("failure")
                    .message(e.getMessage())
                    .fdAccountNo(request.getFdAccountNo())
                    .build());
        } catch (Exception e) {
            log.error("❌ LAB L15: Failed to process withdrawal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WithdrawalResponse.builder()
                            .status("failure")
                            .message("Failed to process withdrawal: " + e.getMessage())
                            .fdAccountNo(request.getFdAccountNo())
                            .build());
        }
    }
}
