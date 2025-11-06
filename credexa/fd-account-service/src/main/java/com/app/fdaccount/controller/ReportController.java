package com.app.fdaccount.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.fdaccount.dto.FDPortfolioDto;
import com.app.fdaccount.dto.FDReportDto;
import com.app.fdaccount.dto.InterestTransactionReportDto;
import com.app.fdaccount.dto.MaturitySummaryDto;
import com.app.fdaccount.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FD Reporting
 * Provides secure access to various operational and compliance reports
 */
@Slf4j
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
@Tag(name = "FD Reporting", description = "Reporting APIs for FD accounts, transactions, and maturity summaries")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    /**
     * Get FD Summary Report (Admin/Bank Officer only)
     * Aggregated data grouped by product code
     */
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    @GetMapping("/fd-summary")
    @Operation(
        summary = "Get FD Summary Report",
        description = "Get aggregated FD account statistics grouped by product code. " +
                     "Accessible by BANK_OFFICER and ADMIN roles only."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "FD summary generated successfully",
            content = @Content(schema = @Schema(implementation = FDReportDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<FDReportDto>> getFDSummary() {
        log.info("REST: Generating FD summary report");
        List<FDReportDto> report = reportService.getFDSummary();
        log.info("REST: FD summary report generated with {} products", report.size());
        return ResponseEntity.ok(report);
    }

    /**
     * Get Customer Portfolio Report (Customer only - own data)
     * All FD accounts for the authenticated customer
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/customer-portfolio")
    @Operation(
        summary = "Get Customer Portfolio Report",
        description = "Get all FD accounts for the authenticated customer. " +
                     "Customers can only view their own portfolio."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Portfolio retrieved successfully",
            content = @Content(schema = @Schema(implementation = FDPortfolioDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<FDPortfolioDto>> getCustomerPortfolio(Authentication auth) {
        // In a real system, extract customerId from JWT token
        // For now, using username as customerId (should be parsed from token claims)
        String username = auth.getName();
        log.info("REST: Generating portfolio report for user: {}", username);
        
        // TODO: Extract actual customerId from JWT token
        // For demo purposes, assuming customerId = 1
        Long customerId = 1L; // This should come from JWT token claims
        
        List<FDPortfolioDto> portfolio = reportService.getPortfolioForCustomer(customerId);
        log.info("REST: Portfolio report generated with {} accounts", portfolio.size());
        return ResponseEntity.ok(portfolio);
    }

    /**
     * Get Customer Portfolio by Customer ID (Admin/Bank Officer)
     * View any customer's portfolio
     */
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    @GetMapping("/customer-portfolio/admin")
    @Operation(
        summary = "Get Customer Portfolio (Admin)",
        description = "Get FD portfolio for any customer by customer ID. " +
                     "Accessible by BANK_OFFICER and ADMIN roles only."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Portfolio retrieved successfully",
            content = @Content(schema = @Schema(implementation = FDPortfolioDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<FDPortfolioDto>> getCustomerPortfolioAdmin(
            @Parameter(description = "Customer ID", required = true, example = "1")
            @RequestParam Long customerId) {
        log.info("REST: Generating portfolio report for customer: {}", customerId);
        List<FDPortfolioDto> portfolio = reportService.getPortfolioForCustomer(customerId);
        log.info("REST: Portfolio report generated with {} accounts", portfolio.size());
        return ResponseEntity.ok(portfolio);
    }

    /**
     * Get Interest Transaction History (Customer - own data)
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/interest-history")
    @Operation(
        summary = "Get Interest Transaction History",
        description = "Get all interest credit transactions for the authenticated customer within date range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Interest history retrieved successfully",
            content = @Content(schema = @Schema(implementation = InterestTransactionReportDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<InterestTransactionReportDto>> getInterestHistory(
            @Parameter(description = "From date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date (YYYY-MM-DD)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth) {
        
        String username = auth.getName();
        log.info("REST: Generating interest history for user: {} from {} to {}", username, fromDate, toDate);
        
        // TODO: Extract actual customerId from JWT token
        Long customerId = 1L; // This should come from JWT token claims
        
        List<InterestTransactionReportDto> history = reportService.getInterestTransactionHistory(
                customerId, fromDate, toDate);
        log.info("REST: Interest history generated with {} transactions", history.size());
        return ResponseEntity.ok(history);
    }

    /**
     * Get Interest Transaction History for any customer (Admin/Bank Officer)
     */
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    @GetMapping("/interest-history/admin")
    @Operation(
        summary = "Get Interest Transaction History (Admin)",
        description = "Get interest transaction history for any customer by customer ID within date range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Interest history retrieved successfully",
            content = @Content(schema = @Schema(implementation = InterestTransactionReportDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<InterestTransactionReportDto>> getInterestHistoryAdmin(
            @Parameter(description = "Customer ID", required = true, example = "1")
            @RequestParam Long customerId,
            @Parameter(description = "From date (YYYY-MM-DD)", example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date (YYYY-MM-DD)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        log.info("REST: Generating interest history for customer: {} from {} to {}", 
                 customerId, fromDate, toDate);
        
        List<InterestTransactionReportDto> history = reportService.getInterestTransactionHistory(
                customerId, fromDate, toDate);
        log.info("REST: Interest history generated with {} transactions", history.size());
        return ResponseEntity.ok(history);
    }

    /**
     * Get Maturity Summary Report (Admin/Bank Officer)
     * Accounts maturing within date range
     */
    @PreAuthorize("hasRole('BANK_OFFICER') or hasRole('ADMIN')")
    @GetMapping("/maturity-summary")
    @Operation(
        summary = "Get Maturity Summary Report",
        description = "Get all active accounts maturing within the specified date range. " +
                     "Accessible by BANK_OFFICER and ADMIN roles only."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Maturity summary generated successfully",
            content = @Content(schema = @Schema(implementation = MaturitySummaryDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<MaturitySummaryDto>> getMaturitySummary(
            @Parameter(description = "From date (YYYY-MM-DD)", example = "2024-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date (YYYY-MM-DD)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        log.info("REST: Generating maturity summary from {} to {}", fromDate, toDate);
        List<MaturitySummaryDto> summary = reportService.getMaturitySummary(fromDate, toDate);
        log.info("REST: Maturity summary generated with {} accounts", summary.size());
        return ResponseEntity.ok(summary);
    }

    /**
     * Get Maturity Summary for Customer (Customer - own data)
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/maturity-summary/customer")
    @Operation(
        summary = "Get Customer Maturity Summary",
        description = "Get maturity summary for the authenticated customer's accounts within date range"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Maturity summary retrieved successfully",
            content = @Content(schema = @Schema(implementation = MaturitySummaryDto.class))
        ),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions")
    })
    public ResponseEntity<List<MaturitySummaryDto>> getCustomerMaturitySummary(
            @Parameter(description = "From date (YYYY-MM-DD)", example = "2024-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "To date (YYYY-MM-DD)", example = "2024-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth) {
        
        String username = auth.getName();
        log.info("REST: Generating maturity summary for user: {} from {} to {}", username, fromDate, toDate);
        
        // TODO: Extract actual customerId from JWT token
        Long customerId = 1L; // This should come from JWT token claims
        
        List<MaturitySummaryDto> summary = reportService.getMaturitySummaryForCustomer(
                customerId, fromDate, toDate);
        log.info("REST: Maturity summary generated with {} accounts", summary.size());
        return ResponseEntity.ok(summary);
    }
}
