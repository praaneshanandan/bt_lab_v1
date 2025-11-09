package com.app.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.account.dto.InterestCalculationRequest;
import com.app.account.dto.InterestCalculationResponse;
import com.app.account.service.InterestCalculationService;
import com.app.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for Interest Calculation operations
 * Provides endpoint for calculating and crediting interest on FD accounts
 */
@RestController
@RequestMapping("/interest")
@Tag(name = "Interest Calculation", description = "APIs for FD account interest calculation and crediting")
@SecurityRequirement(name = "Bearer Authentication")
public class InterestCalculationController {

    private static final Logger logger = LoggerFactory.getLogger(InterestCalculationController.class);

    @Autowired
    private InterestCalculationService interestCalculationService;

    /**
     * Calculate and optionally credit interest for an FD account
     * 
     * @param request Interest calculation request
     * @return Interest calculation response with complete details
     */
    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(
        summary = "Calculate and credit interest for FD account",
        description = "Calculate interest for a specific period or up to current date. Optionally credit the interest to account (creates INTEREST_CREDIT transaction) and apply TDS deduction (creates TDS_DEDUCTION transaction). Uses simple interest formula: (Principal × Rate × Days) / (100 × 365). Supports automatic period detection from last interest credit."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Interest calculated successfully",
            content = @Content(schema = @Schema(implementation = InterestCalculationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation error or invalid date range"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Account not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - account closed or suspended"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - CUSTOMER role cannot calculate interest"
        )
    })
    public ResponseEntity<ApiResponse<InterestCalculationResponse>> calculateInterest(
            @Valid @RequestBody InterestCalculationRequest request) {
        
        try {
            String currentUser = getCurrentUsername();
            logger.info("💰 Interest calculation request received: Account={}, CreditInterest={}, ApplyTDS={}, User={}", 
                    request.getAccountNumber(), 
                    request.getCreditInterestOrDefault(),
                    request.getApplyTdsOrDefault(),
                    currentUser);

            InterestCalculationResponse response = interestCalculationService.calculateInterest(request, currentUser);

            String successMessage = request.getCreditInterestOrDefault() 
                    ? "Interest calculated and credited successfully" 
                    : "Interest calculated successfully (not credited)";

            return ResponseEntity.ok(
                ApiResponse.success(
                    successMessage,
                    response
                )
            );

        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request: " + e.getMessage())
            );
        } catch (IllegalStateException e) {
            logger.error("❌ Invalid state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error("Invalid state: " + e.getMessage())
            );
        } catch (Exception e) {
            logger.error("❌ Error processing interest calculation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to process interest calculation: " + e.getMessage())
            );
        }
    }

    /**
     * Get current username from security context
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null ? authentication.getName() : "system";
    }
}
