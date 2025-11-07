package com.app.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for user account creation
 * Returns the created user details including temporary password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResponse {

    private Long userId;
    private String username;
    private String email;
    private String mobileNumber;
    private String temporaryPassword;
    private Boolean accountActive;
    private String message;
}
