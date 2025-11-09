package com.app.account.controller;

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

import com.app.account.dto.AccountInquiryRequest.AccountIdType;
import com.app.account.dto.CreateTransactionRequest;
import com.app.account.dto.TransactionInquiryRequest;
import com.app.account.dto.TransactionResponse;
import com.app.account.entity.FdTransaction;
import com.app.account.service.AccountService;
import com.app.account.service.TransactionService;
import com.app.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for FD Transaction Management
 */
@RestController
@RequestMapping("/transactions")
@Tag(name = "Transaction Management", description = "APIs for FD Transaction operations")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    /**
     * Create Transaction - Using Account ID type and value
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Create Transaction",
        description = "Creates a new transaction on an FD account. Supports flexible account ID types " +
                      "(ACCOUNT_NUMBER, IBAN, INTERNAL_ID). Automatically calculates balance before/after."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transaction created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires MANAGER or ADMIN role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Parameter(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", example = "ACCOUNT_NUMBER")
            @RequestParam(defaultValue = "ACCOUNT_NUMBER") AccountIdType idType,
            @Parameter(description = "Account ID value", example = "FD-20251108120000-1234-5", required = true)
            @RequestParam String idValue,
            @Valid @RequestBody CreateTransactionRequest request) {
        try {
            String currentUser = getCurrentUsername();
            logger.info("💳 Creating transaction by user: {} for account: {} ({})", currentUser, idValue, idType);

            TransactionResponse response = transactionService.createTransaction(idType, idValue, request, currentUser);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Transaction created successfully", response));
        } catch (Exception e) {
            logger.error("❌ Error creating transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to create transaction: " + e.getMessage()));
        }
    }

    /**
     * Transaction Inquiry - Using Account ID type, value, and Transaction ID
     */
    @PostMapping("/inquiry")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Transaction Inquiry",
        description = "Find a specific transaction using account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID) " +
                      "and transaction ID. Verifies the transaction belongs to the specified account. " +
                      "Customers can only access their own transactions, while Managers and Admins can access all transactions."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction or account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - customers can only access their own transactions")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> transactionInquiry(
            @RequestBody TransactionInquiryRequest inquiryRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            logger.info("🔍 Transaction inquiry: Account ID Type={}, ID Value={}, Transaction ID={}, User={}", 
                    inquiryRequest.getIdTypeOrDefault(), inquiryRequest.getIdValue(), inquiryRequest.getTransactionId(), username);

            TransactionResponse response = transactionService.getTransactionByInquiry(inquiryRequest);
            
            // Check if customer can access this transaction
            if (!canAccessTransaction(response, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to transaction {} belonging to customerId: {}", 
                        username, response.getTransactionId(), response.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view your own transactions")
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success("Transaction found", response));
        } catch (Exception e) {
            logger.error("❌ Transaction inquiry failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Transaction not found: " + e.getMessage()));
        }
    }

    /**
     * Get transaction by transaction ID (simple lookup)
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get Transaction by ID",
        description = "Retrieve transaction details using transaction ID. Customers can only access their own transactions, while Managers and Admins can access all transactions."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - customers can only access their own transactions")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @Parameter(description = "Transaction ID", example = "TXN-20251108120000-1001")
            @PathVariable String transactionId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            TransactionResponse response = transactionService.getTransactionById(transactionId);
            
            // Check if customer can access this transaction
            if (!canAccessTransaction(response, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to transaction {} belonging to customerId: {}", 
                        username, response.getTransactionId(), response.getCustomerId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view your own transactions")
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", response));
        } catch (Exception e) {
            logger.error("❌ Error fetching transaction {}: {}", transactionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Transaction not found: " + e.getMessage()));
        }
    }

    /**
     * Transaction List - Using Account ID type and value
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List Transactions by Account",
        description = "Get paginated list of transactions for a specific account. " +
                      "Supports flexible account ID types (ACCOUNT_NUMBER, IBAN, INTERNAL_ID). " +
                      "Customers can only access their own account transactions, while Managers and Admins can access all accounts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - customers can only access their own account transactions")
    })
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactionsByAccount(
            @Parameter(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", example = "ACCOUNT_NUMBER")
            @RequestParam(defaultValue = "ACCOUNT_NUMBER") AccountIdType idType,
            @Parameter(description = "Account ID value", example = "FD-20251108120000-1234-5", required = true)
            @RequestParam String idValue,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field", example = "transactionDate")
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDir) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // Check if customer can access this account
            if (!canAccessAccountByIdValue(idType, idValue, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to account {} ({})", 
                        username, idValue, idType);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view transactions for your own accounts")
                );
            }
            
            Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<TransactionResponse> transactions = transactionService.listTransactionsByAccountId(idType, idValue, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d transactions (page %d of %d)", 
                            transactions.getNumberOfElements(), page + 1, transactions.getTotalPages()),
                    transactions));
        } catch (Exception e) {
            logger.error("❌ Error listing transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to list transactions: " + e.getMessage()));
        }
    }

    /**
     * List transactions by account number (standard)
     */
    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List Transactions by Account Number",
        description = "Get paginated list of transactions for a specific account number. Customers can only access their own account transactions, while Managers and Admins can access all accounts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - customers can only access their own account transactions")
    })
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactionsByAccountNumber(
            @Parameter(description = "Account number", example = "FD-20251108120000-1234-5")
            @PathVariable String accountNumber,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // Check if customer can access this account
            if (!canAccessAccountByAccountNumber(accountNumber, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to account {}", 
                        username, accountNumber);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view transactions for your own accounts")
                );
            }
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
            Page<TransactionResponse> transactions = transactionService.listTransactionsByAccountNumber(accountNumber, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d transactions for account %s", transactions.getNumberOfElements(), accountNumber),
                    transactions));
        } catch (Exception e) {
            logger.error("❌ Error listing transactions for account {}: {}", accountNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Failed to list transactions: " + e.getMessage()));
        }
    }

    /**
     * List transactions by type
     */
    @GetMapping("/type/{transactionType}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List Transactions by Type",
        description = "Get paginated list of transactions filtered by type (DEPOSIT, INTEREST_CREDIT, etc.)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactionsByType(
            @Parameter(description = "Transaction type", example = "INTEREST_CREDIT")
            @PathVariable FdTransaction.TransactionType transactionType,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
            Page<TransactionResponse> transactions = transactionService.listTransactionsByType(transactionType, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d %s transactions", transactions.getNumberOfElements(), transactionType),
                    transactions));
        } catch (Exception e) {
            logger.error("❌ Error listing transactions by type {}: {}", transactionType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list transactions: " + e.getMessage()));
        }
    }

    /**
     * List transactions by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "List Transactions by Status",
        description = "Get paginated list of transactions filtered by status (PENDING, COMPLETED, etc.)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> listTransactionsByStatus(
            @Parameter(description = "Transaction status", example = "COMPLETED")
            @PathVariable FdTransaction.TransactionStatus status,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
            Page<TransactionResponse> transactions = transactionService.listTransactionsByStatus(status, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Retrieved %d %s transactions", transactions.getNumberOfElements(), status),
                    transactions));
        } catch (Exception e) {
            logger.error("❌ Error listing transactions by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list transactions: " + e.getMessage()));
        }
    }

    /**
     * Get transaction count for account
     */
    @GetMapping("/count/{accountNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(
        summary = "Get Transaction Count",
        description = "Get total number of transactions for an account. Customers can only access their own account transaction counts, while Managers and Admins can access all accounts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction count retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - customers can only access their own account transaction counts")
    })
    public ResponseEntity<ApiResponse<Long>> getTransactionCount(
            @Parameter(description = "Account number", example = "FD-20251108120000-1234-5")
            @PathVariable String accountNumber) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            // Check if customer can access this account
            if (!canAccessAccountByAccountNumber(accountNumber, authentication)) {
                logger.warn("⚠️ User {} attempted unauthorized access to account {}", 
                        username, accountNumber);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.error("Access denied: You can only view transaction count for your own accounts")
                );
            }
            
            long count = transactionService.getTransactionCount(accountNumber);
            return ResponseEntity.ok(ApiResponse.success(
                    String.format("Account %s has %d transactions", accountNumber, count), count));
        } catch (Exception e) {
            logger.error("❌ Error getting transaction count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to get transaction count: " + e.getMessage()));
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
     * Verify if the authenticated user can access the transaction
     * Customers can only access their own transactions
     * Managers and Admins can access all transactions
     */
    private boolean canAccessTransaction(TransactionResponse transaction, Authentication authentication) {
        if (isAdminOrManager(authentication)) {
            return true;
        }
        
        // For customers, check if transaction belongs to their account
        String username = authentication.getName();
        Long userCustomerId = getCustomerIdForUser(username);
        
        if (userCustomerId == null) {
            logger.warn("⚠️ Customer ID not found for user: {}", username);
            return false;
        }
        
        boolean hasAccess = transaction.getCustomerId().equals(userCustomerId);
        if (!hasAccess) {
            logger.warn("⚠️ User {} (customerId: {}) attempted to access transaction belonging to customerId: {}", 
                    username, userCustomerId, transaction.getCustomerId());
        }
        
        return hasAccess;
    }

    /**
     * Verify if the authenticated user can access the account by account number
     */
    private boolean canAccessAccountByAccountNumber(String accountNumber, Authentication authentication) {
        if (isAdminOrManager(authentication)) {
            return true;
        }
        
        try {
            // Get account details to check customerId
            var account = accountService.getAccountByAccountNumber(accountNumber);
            String username = authentication.getName();
            Long userCustomerId = getCustomerIdForUser(username);
            
            if (userCustomerId == null) {
                logger.warn("⚠️ Customer ID not found for user: {}", username);
                return false;
            }
            
            boolean hasAccess = account.getCustomerId().equals(userCustomerId);
            if (!hasAccess) {
                logger.warn("⚠️ User {} (customerId: {}) attempted to access account {} belonging to customerId: {}", 
                        username, userCustomerId, accountNumber, account.getCustomerId());
            }
            
            return hasAccess;
        } catch (Exception e) {
            logger.error("❌ Error checking account access: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify if the authenticated user can access the account by ID type and value
     */
    private boolean canAccessAccountByIdValue(AccountIdType idType, String idValue, Authentication authentication) {
        if (isAdminOrManager(authentication)) {
            return true;
        }
        
        try {
            // Get account details to check customerId
            var account = accountService.getAccountByIdType(idType, idValue);
            String username = authentication.getName();
            Long userCustomerId = getCustomerIdForUser(username);
            
            if (userCustomerId == null) {
                logger.warn("⚠️ Customer ID not found for user: {}", username);
                return false;
            }
            
            boolean hasAccess = account.getCustomerId().equals(userCustomerId);
            if (!hasAccess) {
                logger.warn("⚠️ User {} (customerId: {}) attempted to access account {} ({}) belonging to customerId: {}", 
                        username, userCustomerId, idValue, idType, account.getCustomerId());
            }
            
            return hasAccess;
        } catch (Exception e) {
            logger.error("❌ Error checking account access: {}", e.getMessage());
            return false;
        }
    }
}
