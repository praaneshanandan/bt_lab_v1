package com.app.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreatedEvent {
    private String accountNumber;
    private String accountName;
    private Long customerId;
    private String customerEmail;
    private String productCode;
    private BigDecimal principalAmount;
    private Double interestRate;
    private Integer termMonths;
    private String status;
    private LocalDateTime createdAt;
    private String eventType;
}
