package com.app.fdaccount.dto;

import com.app.fdaccount.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for adding a role (customer) to an account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Account role information (owner, co-owner, nominee, etc.)")
public class AccountRoleRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Customer ID from customer service", example = "1", required = true)
    private Long customerId;

    @Size(max = 100, message = "Customer name cannot exceed 100 characters")
    @Schema(description = "Customer full name (optional - will be auto-fetched from customer service if not provided)", example = "John Doe")
    private String customerName;

    @NotNull(message = "Role type is required")
    @Schema(description = "Type of role on the account", example = "OWNER", required = true, 
            allowableValues = {"OWNER", "CO_OWNER", "NOMINEE", "AUTHORIZED_SIGNATORY", "GUARDIAN"})
    private RoleType roleType;

    @DecimalMin(value = "0.00", message = "Ownership percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Ownership percentage cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Invalid ownership percentage format")
    @Schema(description = "Ownership percentage (0-100)", example = "100.00")
    private BigDecimal ownershipPercentage;

    @Builder.Default
    @Schema(description = "Whether this is the primary account holder", example = "true")
    private Boolean isPrimary = false;

    @Builder.Default
    @Schema(description = "Whether this role is active", example = "true")
    private Boolean isActive = true;

    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    @Schema(description = "Optional notes about this role", example = "Primary account holder")
    private String remarks;
}
