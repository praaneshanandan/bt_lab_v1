package com.app.fdaccount.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.fdaccount.batch.InterestCalculationBatch;
import com.app.fdaccount.batch.MaturityNoticeBatch;
import com.app.fdaccount.batch.MaturityProcessingBatch;
import com.app.fdaccount.batch.StatementGenerationBatch;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.repository.FdAccountRepository;
import com.app.fdaccount.service.InterestCapitalizationService;
import com.app.fdaccount.service.InterestPayoutService;
import com.app.fdaccount.service.StatementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Batch Controller
 * Provides endpoints for manually triggering batch jobs
 * Access restricted to ADMIN and MANAGER roles
 */
@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Operations", description = "Manual batch job triggers for FD operations")
public class BatchController {

    private final InterestCalculationBatch interestCalculationBatch;
    private final MaturityProcessingBatch maturityProcessingBatch;
    private final MaturityNoticeBatch maturityNoticeBatch;
    private final StatementGenerationBatch statementGenerationBatch;
    private final InterestCapitalizationService capitalizationService;
    private final InterestPayoutService payoutService;
    private final StatementService statementService;
    private final FdAccountRepository accountRepository;

    /**
     * Manually trigger daily interest calculation batch
     */
    @PostMapping("/interest-calculation")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Trigger Interest Calculation Batch",
            description = "Manually triggers the daily interest calculation batch job for all active FD accounts",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> triggerInterestCalculation() {
        log.info("Manual trigger: Interest Calculation Batch");
        
        long startTime = System.currentTimeMillis();
        
        try {
            interestCalculationBatch.calculateDailyInterest();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Interest calculation batch completed successfully");
            response.put("durationMs", duration);
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual interest calculation batch", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Interest calculation batch failed: " + e.getMessage());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger interest capitalization for a specific account
     */
    @PostMapping("/capitalize-interest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Capitalize Interest for Account",
            description = "Manually capitalizes accrued interest to principal for a specific FD account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> capitalizeInterest(
            @RequestParam String accountNumber,
            @RequestParam(required = false, defaultValue = "ADMIN") String performedBy) {
        
        log.info("Manual interest capitalization requested for account: {}", accountNumber);
        
        try {
            FdAccount account = capitalizationService.capitalizeInterest(accountNumber, performedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Interest capitalized successfully");
            response.put("accountNumber", account.getAccountNumber());
            response.put("newPrincipal", account.getPrincipalAmount());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error capitalizing interest for account: {}", accountNumber, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Capitalization failed: " + e.getMessage());
            response.put("accountNumber", accountNumber);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Manually trigger interest payout for a specific account
     */
    @PostMapping("/payout-interest")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Process Interest Payout for Account",
            description = "Manually processes interest payout for a specific FD account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> payoutInterest(
            @RequestParam String accountNumber,
            @RequestParam(required = false, defaultValue = "ADMIN") String performedBy) {
        
        log.info("Manual interest payout requested for account: {}", accountNumber);
        
        try {
            FdAccount account = payoutService.processInterestPayout(accountNumber, performedBy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Interest payout processed successfully");
            response.put("accountNumber", account.getAccountNumber());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing payout for account: {}", accountNumber, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Payout failed: " + e.getMessage());
            response.put("accountNumber", accountNumber);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Manually trigger maturity processing batch
     */
    @PostMapping("/maturity-processing")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Trigger Maturity Processing Batch",
            description = "Manually triggers the maturity processing batch job for all matured FD accounts",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> triggerMaturityProcessing() {
        log.info("Manual trigger: Maturity Processing Batch");
        
        long startTime = System.currentTimeMillis();
        
        try {
            maturityProcessingBatch.processMaturedAccounts();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Maturity processing batch completed successfully");
            response.put("durationMs", duration);
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual maturity processing batch", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Maturity processing batch failed: " + e.getMessage());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger maturity notice batch
     */
    @PostMapping("/maturity-notice")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Trigger Maturity Notice Batch",
            description = "Manually triggers the maturity notice batch job to send notices for upcoming maturities",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> triggerMaturityNotice() {
        log.info("Manual trigger: Maturity Notice Batch");
        
        long startTime = System.currentTimeMillis();
        
        try {
            maturityNoticeBatch.sendMaturityNotices();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Maturity notice batch completed successfully");
            response.put("durationMs", duration);
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual maturity notice batch", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Maturity notice batch failed: " + e.getMessage());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger daily statement generation batch
     */
    @PostMapping("/generate-daily-statements")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Generate Daily Statements",
            description = "Manually triggers daily statement generation for all active FD accounts",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> generateDailyStatements() {
        log.info("Manual trigger: Daily Statement Generation");
        
        long startTime = System.currentTimeMillis();
        
        try {
            statementGenerationBatch.generateDailyStatements();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Daily statement generation completed successfully");
            response.put("durationMs", duration);
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual daily statement generation", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Daily statement generation failed: " + e.getMessage());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger monthly statement generation batch
     */
    @PostMapping("/generate-monthly-statements")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Generate Monthly Statements",
            description = "Manually triggers monthly statement generation for all active FD accounts",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> generateMonthlyStatements() {
        log.info("Manual trigger: Monthly Statement Generation");
        
        long startTime = System.currentTimeMillis();
        
        try {
            statementGenerationBatch.generateMonthlyStatements();
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Monthly statement generation completed successfully");
            response.put("durationMs", duration);
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in manual monthly statement generation", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Monthly statement generation failed: " + e.getMessage());
            response.put("timestamp", LocalDate.now());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get batch job status and statistics
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(
            summary = "Get Batch Job Status",
            description = "Returns current statistics for batch processing",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        log.info("Fetching batch job status");
        
        try {
            List<FdAccount> activeAccounts = accountRepository.findAllActiveAccounts();
            long maturedCount = accountRepository.countByStatus(com.app.fdaccount.enums.AccountStatus.MATURED);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeAccounts", activeAccounts.size());
            stats.put("maturedAccounts", maturedCount);
            stats.put("totalAccounts", accountRepository.count());
            stats.put("timestamp", LocalDate.now());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("statistics", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching batch status", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Failed to fetch batch status: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
