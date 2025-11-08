package com.app.product.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateMatrixResponse {
    
    private Long id;
    private String customerClassification;
    private BigDecimal interestRate;
    private BigDecimal additionalRate;
    private LocalDate effectiveDate;
    private BigDecimal totalRate; // computed: interestRate + additionalRate
}
