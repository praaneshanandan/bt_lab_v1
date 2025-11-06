package com.app.calculator.controller;

import java.math.BigDecimal;
import java.util.List; // Lab L11

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.calculator.dto.CalculationResponse;
import com.app.calculator.dto.ComparisonRequest;
import com.app.calculator.dto.ComparisonResponse;
import com.app.calculator.dto.ProductBasedCalculationRequest;
import com.app.calculator.dto.StandaloneCalculationRequest;
import com.app.calculator.service.FdCalculatorService;
import com.app.calculator.service.FdReportService; // Lab L11
import com.app.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for FD Calculator operations
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FD Calculator", description = "Fixed Deposit calculation and simulation endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class FdCalculatorController {
    
    private final FdCalculatorService fdCalculatorService;
    private final FdReportService fdReportService; // Lab L11
    
    /**
     * Lab L6 Specification: /api/fd/calculate endpoint
     * Lab L10 Enhancement: Only CUSTOMER role can access, fetches user categories
     * Calculate FD with categories support and rate capping logic
     */
    @PostMapping("/fd/calculate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Calculate FD maturity (Lab L6 + Lab L10 Specification)",
        description = "Lab L10 enhanced endpoint: Calculate FD maturity amount with personalized rates. " +
                     "Requires CUSTOMER role. Automatically fetches logged-in user's categories (EMPLOYEE, SENIOR_CITIZEN, etc.) " +
                     "and applies additional interest benefits. Categories provide 0.25% extra per category (max 2%)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Calculation successful with personalized rates",
            content = @Content(schema = @Schema(implementation = CalculationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have CUSTOMER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters or rate cap exceeded"
        )
    })
    public ResponseEntity<ApiResponse<CalculationResponse>> calculateFD(
            @Valid @RequestBody @Parameter(description = "FD calculation request with categories")
            StandaloneCalculationRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Lab L10 /api/fd/calculate request from user: {} - Principal: {}, Term: {} {}, BaseRate: {}, Categories: {}", 
                username,
                request.getPrincipalAmount(), 
                request.getTenure(), 
                request.getTenureUnit(),
                request.getInterestRate(),
                request.getCustomerClassifications());
        
        try {
            // Lab L10: Fetch customer categories from customer service
            // This will be done in service layer
            
            // Validate rate cap logic before calculation
            if (request.getCustomerClassifications() != null && !request.getCustomerClassifications().isEmpty()) {
                // Calculate total additional rate
                BigDecimal additionalRate = BigDecimal.valueOf(request.getCustomerClassifications().size() * 0.25);
                BigDecimal maxAllowed = BigDecimal.valueOf(2.0);
                
                if (additionalRate.compareTo(maxAllowed) > 0) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                        "Rate Cap Exceeded",
                        String.format("Combined additional rate %.2f%% exceeds maximum allowed cap of 2%%", 
                                    additionalRate)
                    ));
                }
                
                // Log effective rate calculation
                BigDecimal effectiveRate = request.getInterestRate().add(additionalRate);
                log.info("Effective rate calculation for {}: Base {}% + Additional {}% = {}%", 
                        username, request.getInterestRate(), additionalRate, effectiveRate);
            }
            
            CalculationResponse response = fdCalculatorService.calculateStandaloneWithAuth(request, username);
            
            // Format response for Lab L10 specification
            return ResponseEntity.ok(ApiResponse.success(
                String.format("FD calculation completed for %s - Maturity: ₹%s, Effective Rate: %.2f%%, Interest Earned: ₹%s",
                            username,
                            response.getMaturityAmount(),
                            response.getInterestRate(),
                            response.getInterestEarned()),
                response
            ));
        } catch (Exception e) {
            log.error("Error in FD calculation for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Calculation Error",
                e.getMessage()
            ));
        }
    }
    
    @PostMapping("/calculate/standalone")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Calculate FD with standalone inputs",
        description = "Calculate FD maturity amount and interest with manual inputs (no product required). " +
                     "User provides all parameters including principal, rate, tenure, and calculation type. " +
                     "Requires CUSTOMER role."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Calculation successful",
            content = @Content(schema = @Schema(implementation = CalculationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have CUSTOMER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input parameters"
        )
    })
    public ResponseEntity<ApiResponse<CalculationResponse>> calculateStandalone(
            @Valid @RequestBody @Parameter(description = "Standalone calculation request with all parameters")
            StandaloneCalculationRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Standalone calculation request from user: {} for principal: {}", username, request.getPrincipalAmount());
        
        CalculationResponse response = fdCalculatorService.calculateStandaloneWithAuth(request, username);
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("FD calculation completed successfully for %s", username),
            response
        ));
    }
    
    @PostMapping("/calculate/product-based")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Calculate FD using product defaults",
        description = "Calculate FD maturity using product configuration from product-pricing-service. " +
                     "Fetches interest rates, TDS settings, and other defaults from the selected product. " +
                     "Allows customization within product limits (max 2% additional rate). " +
                     "Requires CUSTOMER role."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Calculation successful",
            content = @Content(schema = @Schema(implementation = CalculationResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have CUSTOMER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or product not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Product not found"
        )
    })
    public ResponseEntity<ApiResponse<CalculationResponse>> calculateWithProduct(
            @Valid @RequestBody @Parameter(description = "Product-based calculation request")
            ProductBasedCalculationRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Product-based calculation request from user: {} for product ID: {}", username, request.getProductId());
        
        try {
            CalculationResponse response = fdCalculatorService.calculateWithProductAuth(request, username);
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("FD calculation with product defaults completed successfully for %s", username),
                response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Validation Error",
                e.getMessage()
            ));
        } catch (RuntimeException e) {
            log.error("Error calculating with product for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(
                "Calculation Error",
                "Failed to calculate FD: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/compare")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Compare multiple FD scenarios",
        description = "Compare multiple FD calculation scenarios side-by-side. " +
                     "Provides detailed comparison and identifies the best scenario (highest maturity amount). " +
                     "Useful for evaluating different tenures, interest rates, or calculation types. " +
                     "Requires CUSTOMER role."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Comparison successful",
            content = @Content(schema = @Schema(implementation = ComparisonResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token missing or invalid"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have CUSTOMER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid comparison request"
        )
    })
    public ResponseEntity<ApiResponse<ComparisonResponse>> compareScenarios(
            @Valid @RequestBody @Parameter(description = "List of scenarios to compare")
            ComparisonRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Comparison request from user: {} for {} scenarios", username, request.getScenarios().size());
        
        ComparisonResponse response = fdCalculatorService.compareScenarios(request);
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("Successfully compared %d FD scenarios for %s", response.getScenarios().size(), username),
            response
        ));
    }
    
    /**
     * Lab L11: Generate FD calculation report using Python script
     * POST /calculate/report
     * Requires CUSTOMER role and valid JWT authentication
     * Executes Python script via Runtime.exec() to generate CSV report
     */
    @PostMapping("/report")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Generate FD Calculation Report (Lab L11)",
        description = "Generate CSV report from FD calculation data using Python script execution. " +
                     "Requires CUSTOMER role. Executes Python script via Runtime.exec() to create CSV report."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Report generated successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Invalid calculation data"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing JWT token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "Forbidden - User does not have CUSTOMER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500", 
            description = "Report generation failed"
        )
    })
    public ResponseEntity<ApiResponse<String>> generateReport(
            @RequestBody @Parameter(description = "List of FD calculations to include in report") 
            List<CalculationResponse> calculations,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("Lab L11: Generating report for user: {} with {} calculations", username, calculations.size());
        
        try {
            String reportPath = fdReportService.generateReport(calculations, username);
            
            if (reportPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to generate report. Check logs for details."));
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                String.format("Report generated successfully for %s with %d calculations", username, calculations.size()),
                reportPath
            ));
            
        } catch (Exception e) {
            log.error("Lab L11: Error generating report for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error generating report: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Check if the FD Calculator service is running"
    )
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success(
            "Service is healthy",
            "FD Calculator Service is running"
        ));
    }
}
