package com.app.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Account Inquiry with flexible ID type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account inquiry request with ID type support")
public class AccountInquiryRequest {

    @Schema(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", 
            example = "ACCOUNT_NUMBER", 
            defaultValue = "ACCOUNT_NUMBER")
    private AccountIdType idType;

    @Schema(description = "Account ID value", 
            example = "FD-20251108-1234-5", 
            required = true)
    private String idValue;

    public enum AccountIdType {
        ACCOUNT_NUMBER,  // Standard FD account number
        IBAN,            // IBAN format
        INTERNAL_ID      // Database internal ID
    }

    /**
     * Get ID type with default fallback
     */
    public AccountIdType getIdTypeOrDefault() {
        return idType != null ? idType : AccountIdType.ACCOUNT_NUMBER;
    }
}
