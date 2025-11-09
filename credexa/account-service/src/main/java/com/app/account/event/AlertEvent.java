package com.app.account.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published for customer alerts/notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private Long customerId;
    private String customerEmail;
    private String alertType; // ACCOUNT_CREATED, TRANSACTION_COMPLETED, MATURITY_APPROACHING, etc.
    private String subject;
    private String message;
    private String accountNumber;
    private String severity; // INFO, WARNING, CRITICAL
    private LocalDateTime timestamp;
    @Builder.Default
    private String eventType = "ALERT";
}
