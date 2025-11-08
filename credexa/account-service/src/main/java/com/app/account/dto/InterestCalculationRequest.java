package com.app.account.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Interest Calculation
 * Supports calculating interest for a specific period or up to current date
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Interest calculation request for FD account")
public class InterestCalculationRequest {

    @NotBlank(message = "Account number is required")
    @Schema(description = "FD account number", 
            example = "FD-20251108120000-1234-5", 
            required = true)
    private String accountNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Calculate interest from this date (defaults to last interest credit date or effective date)", 
            example = "2025-05-08")
    private LocalDate fromDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Calculate interest up to this date (defaults to current date)", 
            example = "2025-11-08")
    private LocalDate toDate;

    @Schema(description = "Whether to credit the calculated interest to account (creates INTEREST_CREDIT transaction)", 
            example = "true", 
            defaultValue = "false")
    private Boolean creditInterest;

    @Schema(description = "Whether to apply TDS deduction (creates TDS_DEDUCTION transaction)", 
            example = "true", 
            defaultValue = "false")
    private Boolean applyTds;

    @Schema(description = "Payment reference for interest credit", 
            example = "INT-2025-Q4")
    private String paymentReference;

    @Schema(description = "Remarks for the transaction", 
            example = "Quarterly interest calculation")
    private String remarks;

    /**
     * Get creditInterest flag with default
     */
    public Boolean getCreditInterestOrDefault() {
        return creditInterest != null ? creditInterest : false;
    }

    /**
     * Get applyTds flag with default
     */
    public Boolean getApplyTdsOrDefault() {
        return applyTds != null ? applyTds : false;
    }
}
