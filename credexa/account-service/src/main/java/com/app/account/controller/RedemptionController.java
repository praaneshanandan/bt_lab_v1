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

import com.app.account.dto.RedemptionInquiryRequest;
import com.app.account.dto.RedemptionInquiryResponse;
import com.app.account.dto.RedemptionProcessRequest;
import com.app.account.dto.RedemptionProcessResponse;
import com.app.account.service.RedemptionService;
import com.app.account.util.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for FD Redemption operations
 * Provides endpoints for redemption inquiry and processing
 */
@RestController
@RequestMapping("/api/redemptions")
@Tag(name = "Redemption Management", description = "APIs for FD account redemption inquiry and processing")
@SecurityRequirement(name = "bearerAuth")
public class RedemptionController {

    private static final Logger logger = LoggerFactory.getLogger(RedemptionController.class);

    @Autowired
    private RedemptionService redemptionService;

    /**
     * Get redemption inquiry details for an account
     * 
     * @param request Redemption inquiry request with account ID type
     * @return Redemption inquiry response with complete calculation details
     */
    @PostMapping("/inquiry")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @Operation(
        summary = "Get redemption inquiry details",
        description = "Retrieve complete redemption calculation including interest earned, TDS, penalties, and net redemption amount. Supports flexible account identification using ACCOUNT_NUMBER, IBAN, or INTERNAL_ID."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Redemption inquiry retrieved successfully",
            content = @Content(schema = @Schema(implementation = RedemptionInquiryResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation error"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Account not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        )
    })
    public ResponseEntity<ApiResponse<RedemptionInquiryResponse>> getRedemptionInquiry(
            @Valid @RequestBody RedemptionInquiryRequest request) {
        
        try {
            logger.info("📊 Redemption inquiry request received: idType={}, idValue={}", 
                    request.getIdTypeOrDefault(), request.getIdValue());

            RedemptionInquiryResponse response = redemptionService.getRedemptionInquiry(request);

            return ResponseEntity.ok(
                ApiResponse.success(
                    response, 
                    "Redemption inquiry retrieved successfully"
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
            logger.error("❌ Error processing redemption inquiry: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to process redemption inquiry: " + e.getMessage())
            );
        }
    }

    /**
     * Process redemption (full or partial)
     * 
     * @param request Redemption process request
     * @return Redemption process response with transaction details
     */
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(
        summary = "Process FD redemption",
        description = "Process full or partial redemption of FD account. Creates redemption transaction, updates account status, and calculates final payout amount including interest, TDS, and penalties. Full redemption closes the account, while partial redemption maintains active status with reduced balance."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Redemption processed successfully",
            content = @Content(schema = @Schema(implementation = RedemptionProcessResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation error or insufficient balance"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Account not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - account already closed or suspended"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - CUSTOMER role cannot process redemption"
        )
    })
    public ResponseEntity<ApiResponse<RedemptionProcessResponse>> processRedemption(
            @Valid @RequestBody RedemptionProcessRequest request) {
        
        try {
            String currentUser = getCurrentUsername();
            logger.info("💰 Redemption process request received: idType={}, idValue={}, type={}, user={}", 
                    request.getIdTypeOrDefault(), 
                    request.getIdValue(), 
                    request.getRedemptionType(),
                    currentUser);

            RedemptionProcessResponse response = redemptionService.processRedemption(request, currentUser);

            return ResponseEntity.ok(
                ApiResponse.success(
                    response, 
                    "Redemption processed successfully. Transaction ID: " + response.getRedemptionTransactionId()
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
            logger.error("❌ Error processing redemption: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("Failed to process redemption: " + e.getMessage())
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
