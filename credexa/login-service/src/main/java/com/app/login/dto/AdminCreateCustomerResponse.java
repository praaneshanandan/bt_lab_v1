package com.app.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for admin creating customer with login account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateCustomerResponse {

    // User account details
    private Long userId;
    private String username;
    private String email;
    private String mobileNumber;
    private String temporaryPassword; // The generated or provided temporary password
    private boolean accountActive;

    // Customer profile details
    private Long customerId;
    private String fullName;
    private String classification;
    private String kycStatus;

    private String message;
}
