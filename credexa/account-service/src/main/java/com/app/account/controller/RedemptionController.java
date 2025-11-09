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
import com.app.account.service.AccountService;
import com.app.account.service.RedemptionService;
import com.app.common.dto.ApiResponse;

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
@RequestMapping("/redemptions")
@Tag(name = "Redemption Management", description = "APIs for FD account redemption inquiry and processing")
@SecurityRequirement(name = "Bearer Authentication")
public class RedemptionController {

    private static final Logger logger = LoggerFactory.getLogger(RedemptionController.class);

    @Autowired
    private RedemptionService redemptionService;

    @Autowired
    private AccountService accountService;

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
        description = "Retrieve complete redemption calculation including interest earned, TDS, penalties, and net redemption amount. Supports flexible account identification using ACCOUNT_NUMBER, IBAN, or INTERNAL_ID. Customers can only access their own accounts, while Managers and Admins can access all accounts."
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
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - customers can only access their own accounts"
        )
    })
    public ResponseEntity<ApiResponse<RedemptionInquiryResponse>> getRedemptionInquiry(
            @Valid @RequestBody RedemptionInquiryRequest request) {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("📊 Redemption inquiry request received: idType={}, idValue={}, user={}", 
                    request.getIdTypeOrDefault(), request.getIdValue(), username);

            RedemptionInquiryResponse response = redemptionService.getRedemptionInquiry(request);

            // Check if customer can access this account
            if (!canAccessRedemptionInquiry(response, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to account belonging to customerId: {}", 
                        username, response.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view redemption details for your own accounts")
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(
                    "Redemption inquiry retrieved successfully",
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
                    "Redemption processed successfully. Transaction ID: " + response.getRedemptionTransactionId(),
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

    /**
     * Check if user is ADMIN or MANAGER
     */
    private boolean isAdminOrManager(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                 auth.getAuthority().equals("ROLE_MANAGER"));
    }

    /**
     * Get customer ID for the authenticated user
     * Returns null if user is not a customer or customer not found
     */
    private Long getCustomerIdForUser(String username) {
        try {
            return accountService.getCustomerIdByUsername(username);
        } catch (Exception e) {
            logger.error("❌ Error getting customer ID for user {}: {}", username, e.getMessage());
            return null;
        }
    }

    /**
     * Verify if the authenticated user can access the redemption inquiry
     * Customers can only access their own accounts
     * Managers and Admins can access all accounts
     */
    private boolean canAccessRedemptionInquiry(RedemptionInquiryResponse response, Authentication authentication) {
        if (isAdminOrManager(authentication)) {
            return true;
        }
        
        // For customers, check if account belongs to them
        String username = authentication.getName();
        Long userCustomerId = getCustomerIdForUser(username);
        
        if (userCustomerId == null) {
            logger.warn("⚠️ Customer ID not found for user: {}", username);
            return false;
        }
        
        boolean hasAccess = response.getCustomerId().equals(userCustomerId);
        if (!hasAccess) {
            logger.warn("⚠️ User {} (customerId: {}) attempted to access redemption inquiry for customerId: {}", 
                    username, userCustomerId, response.getCustomerId());
        }
        
        return hasAccess;
    }
}
