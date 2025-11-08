package com.app.fdaccount.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.fdaccount.dto.AccountResponse;
import com.app.fdaccount.dto.AccountSummaryResponse;
import com.app.fdaccount.dto.CreateAccountRequest;
import com.app.fdaccount.dto.CustomizeAccountRequest;
import com.app.fdaccount.dto.SearchAccountRequest;
import com.app.fdaccount.enums.AccountIdType;
import com.app.fdaccount.service.AccountCreationService;
import com.app.fdaccount.service.AccountInquiryService;
import com.app.fdaccount.service.MaturityClosureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FD Account operations
 * Security: MANAGER role required for account creation
 *          Customers can view only their own accounts
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "FD Account Management", description = "APIs for managing Fixed Deposit accounts")
public class AccountController {

    private final AccountCreationService accountCreationService;
    private final AccountInquiryService accountInquiryService;
    private final MaturityClosureService maturityClosureService;

    /**
     * Create a new FD account with values inherited from product
     * Security: Only MANAGER role can create FD accounts
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Create FD Account", 
               description = "Create a new Fixed Deposit account with values inherited from the product. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {
        
        log.info("REST: Creating FD account for product: {}", request.getProductCode());
        AccountResponse response = accountCreationService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a new FD account with customized values
     * Security: Only MANAGER role can create FD accounts
     */
    @PostMapping("/customize")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Create Customized FD Account",
               description = "Create a new Fixed Deposit account with customized values within product limits. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AccountResponse> createCustomizedAccount(
            @Valid @RequestBody CustomizeAccountRequest request) {
        
        log.info("REST: Creating customized FD account for product: {}", request.getProductCode());
        AccountResponse response = accountCreationService.createCustomizedAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get account by identifier (account number, IBAN, or ID)
     * Security: MANAGER can view all accounts, CUSTOMER can view only their own
     */
    @GetMapping("/{identifier}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get Account",
               description = "Get FD account details by account number, IBAN, or internal ID. Customers can view only their own accounts.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String identifier,
            @Parameter(description = "Type of identifier: ACCOUNT_NUMBER, IBAN, INTERNAL_ID")
            @RequestParam(defaultValue = "ACCOUNT_NUMBER") AccountIdType idType) {
        
        log.info("REST: Fetching account: {} (type: {})", identifier, idType);
        AccountResponse response = accountInquiryService.getAccount(identifier, idType);
        return ResponseEntity.ok(response);
    }

    /**
     * Get account summary by account number
     * Security: MANAGER can view all accounts, CUSTOMER can view only their own
     */
    @GetMapping("/{accountNumber}/summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get Account Summary",
               description = "Get summarized account details for list view. Customers can view only their own accounts.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AccountSummaryResponse> getAccountSummary(
            @PathVariable String accountNumber) {
        
        log.info("REST: Fetching account summary: {}", accountNumber);
        AccountSummaryResponse response = accountInquiryService.getAccountSummary(accountNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts for a specific customer
     * Security: MANAGER can view any customer's accounts, CUSTOMER can view only their own
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'CUSTOMER')")
    @Operation(summary = "Get Customer Accounts",
               description = "Get all FD accounts for a specific customer. Customers can only view their own accounts.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AccountSummaryResponse>> getCustomerAccounts(
            @PathVariable Long customerId) {
        
        log.info("REST: Fetching accounts for customer: {}", customerId);
        List<AccountSummaryResponse> response = accountInquiryService.getAccountsByCustomer(customerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Search accounts with multiple criteria
     * Security: Only MANAGER can search accounts
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Search FD Accounts",
               description = "Search FD accounts with multiple criteria and pagination. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Page<AccountSummaryResponse>> searchAccounts(
            @Valid @RequestBody SearchAccountRequest request) {
        
        log.info("REST: Searching accounts with criteria");
        Page<AccountSummaryResponse> response = accountInquiryService.searchAccounts(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get accounts maturing in next N days
     * Security: Only MANAGER can access this report
     */
    @GetMapping("/maturing")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get Maturing Accounts",
               description = "Get accounts that will mature in the next N days. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AccountSummaryResponse>> getAccountsMaturingInDays(
            @Parameter(description = "Number of days to look ahead")
            @RequestParam(defaultValue = "30") int days) {
        
        log.info("REST: Fetching accounts maturing in {} days", days);
        List<AccountSummaryResponse> response = accountInquiryService.getAccountsMaturingInDays(days);
        return ResponseEntity.ok(response);
    }

    /**
     * Get accounts by product code
     * Security: Only MANAGER can access this report
     */
    @GetMapping("/product/{productCode}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get Accounts by Product",
               description = "Get all accounts for a specific FD product. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AccountSummaryResponse>> getAccountsByProduct(
            @PathVariable String productCode) {
        
        log.info("REST: Fetching accounts for product: {}", productCode);
        List<AccountSummaryResponse> response = accountInquiryService.getAccountsByProduct(productCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Get accounts by branch code
     * Security: Only MANAGER can access this report
     */
    @GetMapping("/branch/{branchCode}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get Accounts by Branch",
               description = "Get all accounts for a specific branch. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<AccountSummaryResponse>> getAccountsByBranch(
            @PathVariable String branchCode) {
        
        log.info("REST: Fetching accounts for branch: {}", branchCode);
        List<AccountSummaryResponse> response = accountInquiryService.getAccountsByBranch(branchCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if account number exists
     * Security: Only MANAGER can check account existence
     */
    @GetMapping("/exists/{accountNumber}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Check Account Exists",
               description = "Check if an account number already exists. Requires MANAGER role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Boolean> accountExists(@PathVariable String accountNumber) {
        log.info("REST: Checking if account exists: {}", accountNumber);
        boolean exists = accountInquiryService.accountExists(accountNumber);
        return ResponseEntity.ok(exists);
    }

    /**
     * Manually close a matured FD account (Lab L19)
     * Security: Only MANAGER or ADMIN role can manually close accounts
     */
    @PostMapping("/manual-close")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Manual Maturity Closure",
               description = "Manually close a matured FD account and process final payout. " +
                             "Validates maturity date, calculates final amount, creates payout transaction. " +
                             "Requires MANAGER or ADMIN role.",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MaturityClosureService.MaturityClosureResponse> closeMaturedAccount(
            @RequestParam @Parameter(description = "FD account number to close") String accountNumber,
            @RequestParam(required = false, defaultValue = "SYSTEM") 
            @Parameter(description = "User initiating the closure") String performedBy) {
        log.info("REST: Manual maturity closure requested for account: {} by {}", accountNumber, performedBy);
        MaturityClosureService.MaturityClosureResponse response = 
            maturityClosureService.closeMaturedAccount(accountNumber, performedBy);
        return ResponseEntity.ok(response);
    }
}
