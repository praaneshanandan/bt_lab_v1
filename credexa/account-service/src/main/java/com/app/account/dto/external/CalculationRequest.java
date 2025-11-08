package com.app.account.dto.external;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for FD calculation from calculator-service
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
    @Min(value = 1, message = "Tenure must be at least 1 month")
    @JsonProperty("tenureMonths")
    @Schema(description = "Tenure in months", example = "12", required = true)
    private Integer tenureMonths;

    @NotBlank(message = "Calculation type is required")
    @JsonProperty("calculationType")
    @Schema(description = "Calculation type (SIMPLE or COMPOUND)", example = "SIMPLE", required = true)
    private String calculationType;

    @JsonProperty("compoundingFrequency")
    @Schema(description = "Compounding frequency for COMPOUND calculation", example = "QUARTERLY")
    private String compoundingFrequency;

    @NotNull(message = "Start date is required")
    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "FD start date", example = "2025-11-08", required = true)
    private LocalDate startDate;

    @JsonProperty("tdsApplicable")
    @Schema(description = "Whether TDS is applicable", example = "true")
    private Boolean tdsApplicable;

    @JsonProperty("tdsRate")
    @Schema(description = "TDS rate (%)", example = "10.0")
    private BigDecimal tdsRate;

    @JsonProperty("customerId")
    @Schema(description = "Customer ID (optional for TDS calculation)", example = "1")
    private Long customerId;
}
