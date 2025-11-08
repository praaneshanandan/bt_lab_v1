package com.app.account.dto;

import com.app.account.dto.AccountInquiryRequest.AccountIdType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Redemption Inquiry
 * Allows flexible account identification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redemption inquiry request with account ID type support")
public class RedemptionInquiryRequest {

    @Schema(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", 
            example = "ACCOUNT_NUMBER", 
            defaultValue = "ACCOUNT_NUMBER")
    private AccountIdType idType;

    @NotBlank(message = "ID value is required")
    @Schema(description = "Account ID value", 
            example = "FD-20251108120000-1234-5", 
            required = true)
    private String idValue;

    /**
     * Get ID type with default fallback
     */
    public AccountIdType getIdTypeOrDefault() {
        return idType != null ? idType : AccountIdType.ACCOUNT_NUMBER;
    }
}
