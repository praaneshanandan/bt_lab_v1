package com.app.account.controller;

import com.app.account.batch.BatchTimeService;
import com.app.account.batch.InterestAccrualBatch;
import com.app.account.batch.InterestCapitalizationBatch;
import com.app.account.batch.MaturityProcessingBatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for manual batch job triggers (ADMIN only)
 */
@RestController
@RequestMapping("/batch")
@Tag(name = "Batch Management", description = "Manual batch job triggers and time travel controls")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class BatchController {

    @Autowired(required = false)
    private InterestAccrualBatch interestAccrualBatch;

    @Autowired(required = false)
    private InterestCapitalizationBatch interestCapitalizationBatch;

    @Autowired(required = false)
    private MaturityProcessingBatch maturityProcessingBatch;

    @Autowired
    private BatchTimeService batchTimeService;

    @PostMapping("/interest-accrual/trigger")
    @Operation(summary = "Manually trigger interest accrual batch", description = "ADMIN only - Manually run interest accrual for current batch date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch executed successfully"),
            @ApiResponse(responseCode = "503", description = "Batch is disabled in configuration"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> triggerInterestAccrual() {
        Map<String, Object> response = new HashMap<>();
        
        if (interestAccrualBatch == null) {
            response.put("error", "Interest Accrual Batch is disabled. Set batch.interest-accrual.enabled=true");
            return ResponseEntity.status(503).body(response);
        }

        try {
            interestAccrualBatch.processInterestAccrual();
            response.put("message", "Interest Accrual Batch executed successfully");
            response.put("batchDate", batchTimeService.getBatchDate());
            response.put("timeTravelActive", batchTimeService.isTimeTravelActive());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Batch execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/interest-capitalization/trigger")
    @Operation(summary = "Manually trigger interest capitalization batch", description = "ADMIN only - Manually run interest capitalization for current batch date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch executed successfully"),
            @ApiResponse(responseCode = "503", description = "Batch is disabled in configuration"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> triggerInterestCapitalization() {
        Map<String, Object> response = new HashMap<>();
        
        if (interestCapitalizationBatch == null) {
            response.put("error", "Interest Capitalization Batch is disabled. Set batch.interest-capitalization.enabled=true");
            return ResponseEntity.status(503).body(response);
        }

        try {
            interestCapitalizationBatch.processInterestCapitalization();
            response.put("message", "Interest Capitalization Batch executed successfully");
            response.put("batchDate", batchTimeService.getBatchDate());
            response.put("timeTravelActive", batchTimeService.isTimeTravelActive());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Batch execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/maturity-processing/trigger")
    @Operation(summary = "Manually trigger maturity processing batch", description = "ADMIN only - Manually run maturity processing for current batch date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch executed successfully"),
            @ApiResponse(responseCode = "503", description = "Batch is disabled in configuration"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> triggerMaturityProcessing() {
        Map<String, Object> response = new HashMap<>();
        
        if (maturityProcessingBatch == null) {
            response.put("error", "Maturity Processing Batch is disabled. Set batch.maturity-processing.enabled=true");
            return ResponseEntity.status(503).body(response);
        }

        try {
            maturityProcessingBatch.processMaturedAccounts();
            response.put("message", "Maturity Processing Batch executed successfully");
            response.put("batchDate", batchTimeService.getBatchDate());
            response.put("timeTravelActive", batchTimeService.isTimeTravelActive());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Batch execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/time-travel/set")
    @Operation(summary = "Set batch date override (Time Travel)", description = "ADMIN only - Override system date for batch processing. Format: yyyy-MM-dd")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time travel activated"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> setTimeTravelDate(
            @Parameter(description = "Date to override (yyyy-MM-dd)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Map<String, Object> response = new HashMap<>();
        batchTimeService.setOverrideDate(date);
        
        response.put("message", "Time travel activated");
        response.put("overrideDate", date);
        response.put("batchDate", batchTimeService.getBatchDate());
        response.put("timeTravelActive", true);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/time-travel/clear")
    @Operation(summary = "Clear batch date override (Return to present)", description = "ADMIN only - Disable time travel and return to current system date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Time travel deactivated"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> clearTimeTravelDate() {
        Map<String, Object> response = new HashMap<>();
        batchTimeService.clearOverrideDate();
        
        response.put("message", "Time travel deactivated");
        response.put("batchDate", batchTimeService.getBatchDate());
        response.put("timeTravelActive", false);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/time-travel/status")
    @Operation(summary = "Get time travel status", description = "ADMIN only - Check current batch date and time travel status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getTimeTravelStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("timeTravelActive", batchTimeService.isTimeTravelActive());
        response.put("batchDate", batchTimeService.getBatchDate());
        response.put("overrideDate", batchTimeService.getOverrideDate());
        response.put("currentSystemDate", LocalDate.now());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get batch configuration status", description = "ADMIN only - Check which batches are enabled")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<Map<String, Object>> getBatchStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("interestAccrualEnabled", interestAccrualBatch != null);
        response.put("interestCapitalizationEnabled", interestCapitalizationBatch != null);
        response.put("maturityProcessingEnabled", maturityProcessingBatch != null);
        response.put("timeTravelActive", batchTimeService.isTimeTravelActive());
        response.put("currentBatchDate", batchTimeService.getBatchDate());
        
        return ResponseEntity.ok(response);
    }
}
