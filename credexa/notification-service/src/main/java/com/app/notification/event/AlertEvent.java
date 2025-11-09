package com.app.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private Long customerId;
    private String customerEmail;
    private String alertType;
    private String subject;
    private String message;
    private String accountNumber;
    private String severity;
    private LocalDateTime timestamp;
    private String eventType;
}
