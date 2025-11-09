package com.app.account.controller;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.account.dto.AccountInquiryRequest;
import com.app.account.dto.AccountResponse;
import com.app.account.dto.BalanceResponse;
import com.app.account.dto.CreateAccountRequest;
import com.app.account.service.AccountService;
import com.app.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for FD Account Management
 */
@RestController
@RequestMapping("/")
@Tag(name = "Account Management", description = "APIs for Fixed Deposit Account operations")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Check if the account service is running")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Account Service is running", null));
    }

    /**
     * Create FD Account - VERSION 1: Default values from product
     */
    @PostMapping("/create/default")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CUSTOMER_MANAGER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Create FD Account with Default Values",
        description = "Creates a new FD account using all default values from the product configuration. " +
                      "Interest rate, calculation type, and other parameters are taken from the product. " +
                      "Only ADMIN, MANAGER, or CUSTOMER_MANAGER can create accounts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires MANAGER or ADMIN role")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> createAccountWithDefaults(
            @Valid @RequestBody CreateAccountRequest request) {
        try {
            String currentUser = getCurrentUsername();
            logger.info("� Creating account with defaults by user: {}", currentUser);

            AccountResponse response = accountService.createAccountWithDefaults(request, currentUser);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("FD Account created successfully with default values", response));
        } catch (Exception e) {
            logger.error("❌ Error creating account with defaults: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create account: " + e.getMessage()));
        }
    }

    /**
     * Create FD Account - VERSION 2: Customized values within product purview
     */
    @PostMapping("/create/custom")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('CUSTOMER_MANAGER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Create FD Account with Custom Values",
        description = "Creates a new FD account with customized interest rate and calculation parameters. " +
                      "Custom rate must be within ±2% of the product's base rate. " +
                      "Allows customization of calculation type and compounding frequency. " +
                      "Only MANAGER, ADMIN, or CUSTOMER_MANAGER can create accounts with custom parameters."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or custom values outside allowed range"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires MANAGER or ADMIN role")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> createAccountWithCustomization(
            @Valid @RequestBody CreateAccountRequest request,
            @Parameter(description = "Custom interest rate (within ±2% of base rate)", example = "8.5")
            @RequestParam(required = false) BigDecimal customInterestRate,
            @Parameter(description = "Custom calculation type (SIMPLE or COMPOUND)", example = "COMPOUND")
            @RequestParam(required = false) String customCalculationType,
            @Parameter(description = "Custom compounding frequency (MONTHLY, QUARTERLY, ANNUALLY)", example = "MONTHLY")
            @RequestParam(required = false) String customCompoundingFrequency) {
        try {
            String currentUser = getCurrentUsername();
            logger.info("📝 Creating account with customization by user: {} (Rate: {})", currentUser, customInterestRate);

            AccountResponse response = accountService.createAccountWithCustomization(
                    request, customInterestRate, customCalculationType, customCompoundingFrequency, currentUser);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("FD Account created successfully with custom values", response));
        } catch (Exception e) {
            logger.error("❌ Error creating account with customization: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create account: " + e.getMessage()));
        }
    }

    /**
     * Account Inquiry - Find by flexible ID type
     */
    @PostMapping("/inquiry")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Account Inquiry",
        description = "Find account by flexible ID type (ACCOUNT_NUMBER, IBAN, or INTERNAL_ID). " +
                      "If no ID type is specified, defaults to ACCOUNT_NUMBER."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other customer's account")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> accountInquiry(
            @RequestBody AccountInquiryRequest inquiryRequest,
            Authentication authentication) {
        try {
            logger.info("🔍 Account inquiry: Type={}, Value={}", 
                    inquiryRequest.getIdTypeOrDefault(), inquiryRequest.getIdValue());

            AccountResponse response = accountService.getAccountByInquiry(inquiryRequest);
            
            // Security check: Customers can only inquire their own accounts
            if (!canAccessAccount(response, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access Denied: You don't have permission to view this account"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Account found", response));
        } catch (Exception e) {
            logger.error("❌ Account inquiry failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Account not found: " + e.getMessage()));
        }
    }

    /**
     * Get account by account number
     */
    @GetMapping("/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get Account by Number",
        description = "Retrieve account details using standard account number"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other customer's account")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @Parameter(description = "Account number", example = "FD-20251108123456-1234-5")
            @PathVariable String accountNumber,
            Authentication authentication) {
        try {
            AccountResponse response = accountService.getAccountByNumber(accountNumber);
            
            // Security check: Customers can only view their own accounts
            if (!canAccessAccount(response, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access Denied: You don't have permission to view this account"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", response));
        } catch (Exception e) {
            logger.error("❌ Error fetching account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Account not found: " + e.getMessage()));
        }
    }

    /**
     * List all accounts with pagination
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'CUSTOMER_MANAGER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List All Accounts",
        description = "Get paginated list of all FD accounts. Accessible to MANAGER and ADMIN only."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> listAccounts(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<AccountResponse> accounts = accountService.listAccounts(pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d accounts (page %d of %d)", 
                            accounts.getNumberOfElements(), page + 1, accounts.getTotalPages()),
                    accounts));
        } catch (Exception e) {
            logger.error("❌ Error listing accounts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list accounts: " + e.getMessage()));
        }
    }

    /**
     * List accounts by customer
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List Accounts by Customer",
        description = "Get paginated list of accounts for a specific customer"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Cannot access other customer's accounts")
    })
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> listAccountsByCustomer(
            @Parameter(description = "Customer ID", example = "1")
            @PathVariable Long customerId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            // Security check: Customers can only view their own accounts
            if (!isAdminOrManager(authentication)) {
                String username = authentication.getName();
                Long userCustomerId = getCustomerIdForUser(username);
                
                if (userCustomerId == null || !userCustomerId.equals(customerId)) {
                    logger.warn("⚠️ User {} (customerId: {}) attempted to access accounts for customerId: {}", 
                            username, userCustomerId, customerId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.error("Access Denied: You can only view your own accounts"));
                }
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<AccountResponse> accounts = accountService.listAccountsByCustomer(customerId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d accounts for customer %d", accounts.getNumberOfElements(), customerId),
                    accounts));
        } catch (Exception e) {
            logger.error("❌ Error listing accounts for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list accounts: " + e.getMessage()));
        }
    }

    /**
     * Get account balance
     */
    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get Account Balance",
        description = "Retrieve balance and maturity details for an account"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(description = "Account number", example = "FD-20251108123456-1234-5")
            @PathVariable String accountNumber,
            Authentication authentication) {
        try {
            // First get the account to check ownership
            AccountResponse account = accountService.getAccountByNumber(accountNumber);
            
            // Security check: Customers can only view their own account balance
            if (!canAccessAccount(account, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Access Denied: You don't have permission to view this account balance"));
            }
            
            BalanceResponse response = accountService.getAccountBalance(accountNumber);
            return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully", response));
        } catch (Exception e) {
            logger.error("❌ Error fetching balance for {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to fetch balance: " + e.getMessage()));
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
     * Check if current user has ADMIN or MANAGER role
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
     * Verify if the authenticated user can access the account
     * Customers can only access their own accounts
     * Managers and Admins can access all accounts
     */
    private boolean canAccessAccount(AccountResponse account, Authentication authentication) {
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
        
        boolean hasAccess = account.getCustomerId().equals(userCustomerId);
        if (!hasAccess) {
            logger.warn("⚠️ User {} (customerId: {}) attempted to access account belonging to customerId: {}", 
                    username, userCustomerId, account.getCustomerId());
        }
        
        return hasAccess;
    }
}
