package com.app.account.dto.external;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for FD calculation from calculator-service
 * Matches StandaloneCalculationRequest structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for FD maturity calculation")
public class CalculationRequest {

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.01", message = "Principal must be positive")
    @JsonProperty("principalAmount")
    @Schema(description = "Principal amount", example = "50000", required = true)
    private BigDecimal principalAmount;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
    @JsonProperty("interestRate")
    @Schema(description = "Annual interest rate (%)", example = "7.5", required = true)
    private BigDecimal interestRate;

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Tenure must be at least 1")
    @JsonProperty("tenure")
    @Schema(description = "Investment tenure", example = "12", required = true)
    private Integer tenure;

    @NotNull(message = "Tenure unit is required")
    @JsonProperty("tenureUnit")
    @Schema(description = "Unit of tenure (DAYS, MONTHS, YEARS)", example = "MONTHS", required = true)
    private String tenureUnit;

    @NotNull(message = "Calculation type is required")
    @JsonProperty("calculationType")
    @Schema(description = "Type of interest calculation (SIMPLE, COMPOUND)", example = "COMPOUND", required = true)
    private String calculationType;

    @JsonProperty("compoundingFrequency")
    @Schema(description = "Compounding frequency (MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY)", example = "QUARTERLY")
    private String compoundingFrequency;

    @JsonProperty("tdsRate")
    @Schema(description = "TDS rate (%)", example = "10.0")
    private BigDecimal tdsRate;

    @JsonProperty("customerClassifications")
    @Schema(description = "Customer classifications for additional interest", example = "[\"SENIOR_CITIZEN\"]")
    private List<String> customerClassifications;
}
