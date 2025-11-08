package com.app.account.dto;

import com.app.account.dto.AccountInquiryRequest.AccountIdType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Transaction Inquiry with flexible account ID type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction inquiry request with account ID type support")
public class TransactionInquiryRequest {

    @Schema(description = "Account ID type (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)", 
            example = "ACCOUNT_NUMBER", 
            defaultValue = "ACCOUNT_NUMBER")
    private AccountIdType idType;

    @Schema(description = "Account ID value", 
            example = "FD-20251108-1234-5", 
            required = true)
    private String idValue;

    @Schema(description = "Transaction ID to inquire about", 
            example = "TXN-20251108-001", 
            required = true)
    private String transactionId;

    /**
     * Get ID type with default fallback
     */
    public AccountIdType getIdTypeOrDefault() {
        return idType != null ? idType : AccountIdType.ACCOUNT_NUMBER;
    }
}
