package com.app.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.app.account.dto.AccountInquiryRequest.AccountIdType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Processing Redemption
 * Allows flexible account identification and redemption options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redemption process request")
public class RedemptionProcessRequest {

    @Schema(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", 
            example = "ACCOUNT_NUMBER", 
            defaultValue = "ACCOUNT_NUMBER")
    private AccountIdType idType;

    @NotBlank(message = "ID value is required")
    @Schema(description = "Account ID value", 
            example = "FD-20251108120000-1234-5", 
            required = true)
    private String idValue;

    @NotNull(message = "Redemption type is required")
    @Schema(description = "Redemption type: FULL, PARTIAL", 
            example = "FULL", 
            required = true,
            allowableValues = {"FULL", "PARTIAL"})
    private RedemptionTypeEnum redemptionType;

    @DecimalMin(value = "0.01", message = "Redemption amount must be greater than 0")
    @Schema(description = "Redemption amount (required for PARTIAL redemption)", 
            example = "50000.00")
    private BigDecimal redemptionAmount;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    @Schema(description = "Payment reference number", 
            example = "PAY-2025110812345")
    private String paymentReference;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Schema(description = "Redemption remarks", 
            example = "Full redemption on maturity")
    private String remarks;

    @Schema(description = "Redemption date/time (defaults to now)", 
            example = "2025-11-08T12:00:00")
    private LocalDateTime redemptionDate;

    @Size(max = 50, message = "Channel cannot exceed 50 characters")
    @Schema(description = "Transaction channel", 
            example = "BRANCH", 
            allowableValues = {"API", "BRANCH", "MOBILE", "INTERNET_BANKING"})
    private String channel;

    @Size(max = 50, message = "Branch code cannot exceed 50 characters")
    @Schema(description = "Branch code where redemption is processed", 
            example = "BR001")
    private String branchCode;

    @Schema(description = "Force redemption without penalty check (ADMIN only)", 
            example = "false")
    private Boolean forceRedemption;

    public enum RedemptionTypeEnum {
        FULL,     // Complete account closure
        PARTIAL   // Partial withdrawal
    }

    /**
     * Get ID type with default fallback
     */
    public AccountIdType getIdTypeOrDefault() {
        return idType != null ? idType : AccountIdType.ACCOUNT_NUMBER;
    }
}
