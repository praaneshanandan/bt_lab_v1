package com.app.fdaccount.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for product role data from product-pricing-service
 * Maps to ProductRoleResponse from product-pricing-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRoleDto {
    
    private Long id;
    private String roleType; // OWNER, CO_OWNER, NOMINEE, etc.
    private String description;
}
