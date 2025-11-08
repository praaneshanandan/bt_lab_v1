package com.app.account.dto.external;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for FD calculation from calculator-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FD calculation response from calculator-service")
public class CalculationResponse {

    @JsonProperty("principalAmount")
    @Schema(description = "Principal amount")
    private BigDecimal principalAmount;

    @JsonProperty("interestRate")
    @Schema(description = "Interest rate (%)")
    private BigDecimal interestRate;

    @JsonProperty("tenureMonths")
    @Schema(description = "Tenure in months")
    private Integer tenureMonths;

    @JsonProperty("calculationType")
    @Schema(description = "Calculation type")
    private String calculationType;

    @JsonProperty("compoundingFrequency")
    @Schema(description = "Compounding frequency")
    private String compoundingFrequency;

    @JsonProperty("maturityAmount")
    @Schema(description = "Maturity amount")
    private BigDecimal maturityAmount;

    @JsonProperty("interestEarned")
    @Schema(description = "Total interest earned")
    private BigDecimal interestEarned;

    @JsonProperty("tdsAmount")
    @Schema(description = "TDS amount deducted")
    private BigDecimal tdsAmount;

    @JsonProperty("tdsRate")
    @Schema(description = "TDS rate (%)")
    private BigDecimal tdsRate;

    @JsonProperty("netAmount")
    @Schema(description = "Net amount after TDS")
    private BigDecimal netAmount;

    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "FD start date")
    private LocalDate startDate;

    @JsonProperty("maturityDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "FD maturity date")
    private LocalDate maturityDate;

    @JsonProperty("effectiveInterestRate")
    @Schema(description = "Effective interest rate")
    private BigDecimal effectiveInterestRate;
}
