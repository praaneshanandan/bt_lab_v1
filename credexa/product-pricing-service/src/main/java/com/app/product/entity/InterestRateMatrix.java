package com.app.product.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Interest Rate Matrix Entity
 * Defines interest rate slabs based on amount, term, and customer classification
 */
@Entity
@Table(name = "interest_rate_matrix")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestRateMatrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Customer classification (REGULAR, PREMIUM, SENIOR_CITIZEN, SUPER_SENIOR, VIP)
    @Column(length = 50)
    private String customerClassification;

    // Interest rate for this slab
    @NotNull(message = "Interest rate is required")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    // Additional rate for special cases
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal additionalRate = BigDecimal.ZERO;

    // Effective date for this rate
    @NotNull(message = "Effective date is required")
    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column
    @Builder.Default
    private Boolean active = true;

    /**
     * Check if this rate is applicable for given criteria
     */
    public boolean isApplicable(String classification, LocalDate date) {
        // Check effective date
        if (effectiveDate.isAfter(date)) {
            return false;
        }
        if (!active) {
            return false;
        }

        // Check classification
        if (customerClassification != null && !customerClassification.isEmpty()) {
            if (classification == null || !customerClassification.equalsIgnoreCase(classification)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get total interest rate (base + additional)
     */
    public BigDecimal getTotalRate() {
        return interestRate.add(additionalRate != null ? additionalRate : BigDecimal.ZERO);
    }
}
