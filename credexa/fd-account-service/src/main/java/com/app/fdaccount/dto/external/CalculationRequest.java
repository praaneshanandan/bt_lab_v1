package com.app.fdaccount.dto.external;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for FD calculation from fd-calculator-service
 * Maps to StandaloneCalculationRequest in Calculator Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationRequest {

    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenure;  // Renamed from termMonths
    
    @JsonProperty("tenureUnit")
    private String tenureUnit;  // "MONTHS", "DAYS", "YEARS" - Will be sent as string, calculator accepts enum
    
    @JsonProperty("calculationType")
    private String calculationType;  // "SIMPLE" or "COMPOUND" - Will be sent as string, calculator accepts enum
    
    @JsonProperty("compoundingFrequency")
    private String compoundingFrequency;  // "MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY" - Will be sent as string, calculator accepts enum
    
    private BigDecimal tdsRate;  // Optional TDS rate (default 10%)
    private List<String> customerClassifications;  // Optional (e.g., "SENIOR_CITIZEN")
}
