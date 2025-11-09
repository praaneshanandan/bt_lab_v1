package com.app.notification.consumer;

import com.app.notification.event.AccountCreatedEvent;
import com.app.notification.event.AlertEvent;
import com.app.notification.event.TransactionEvent;
import com.app.notification.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Kafka consumer for account-related events
 */
@Component
public class AccountEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AccountEventConsumer.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    /**
     * Listen to account-created topic
     */
    @KafkaListener(topics = "${kafka.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAccountCreatedEvent(String message) {
        try {
            logger.info("📩 Received AccountCreatedEvent from Kafka");
            
            AccountCreatedEvent event = objectMapper.readValue(message, AccountCreatedEvent.class);
            
            // Prepare email variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("accountNumber", event.getAccountNumber());
            variables.put("accountName", event.getAccountName());
            variables.put("principalAmount", currencyFormatter.format(event.getPrincipalAmount()));
            variables.put("interestRate", event.getInterestRate() + "%");
            variables.put("termMonths", event.getTermMonths());
            variables.put("createdAt", event.getCreatedAt().format(dateFormatter));
            variables.put("productCode", event.getProductCode());
            
            // Send email
            String subject = "🎉 FD Account Created Successfully - " + event.getAccountNumber();
            emailService.sendHtmlEmail(
                    event.getCustomerEmail(),
                    subject,
                    "account-created",
                    variables
            );
            
            logger.info("✅ Account creation email queued for: {}", event.getCustomerEmail());
            
        } catch (Exception e) {
            logger.error("❌ Error processing AccountCreatedEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to customer-alert topic
     */
    @KafkaListener(topics = "${kafka.topics.alert}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeAlertEvent(String message) {
        try {
            logger.info("📩 Received AlertEvent from Kafka");
            
            AlertEvent event = objectMapper.readValue(message, AlertEvent.class);
            
            // Prepare email variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("alertType", event.getAlertType());
            variables.put("subject", event.getSubject());
            variables.put("message", event.getMessage());
            variables.put("accountNumber", event.getAccountNumber());
            variables.put("severity", event.getSeverity());
            variables.put("timestamp", event.getTimestamp().format(dateFormatter));
            
            // Send email
            emailService.sendHtmlEmail(
                    event.getCustomerEmail(),
                    event.getSubject(),
                    "alert",
                    variables
            );
            
            logger.info("✅ Alert email queued for: {}", event.getCustomerEmail());
            
        } catch (Exception e) {
            logger.error("❌ Error processing AlertEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen to account-transaction topic
     */
    @KafkaListener(topics = "${kafka.topics.transaction}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTransactionEvent(String message) {
        try {
            logger.info("📩 Received TransactionEvent from Kafka");
            
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            
            // Skip batch transactions (already covered by alerts)
            if (event.getTransactionType().contains("BATCH")) {
                logger.debug("⏭️ Skipping batch transaction notification");
                return;
            }
            
            // Prepare email variables
            Map<String, Object> variables = new HashMap<>();
            variables.put("transactionId", event.getTransactionId());
            variables.put("accountNumber", event.getAccountNumber());
            variables.put("transactionType", event.getTransactionType());
            variables.put("amount", currencyFormatter.format(event.getAmount()));
            variables.put("balanceBefore", currencyFormatter.format(event.getBalanceBefore()));
            variables.put("balanceAfter", currencyFormatter.format(event.getBalanceAfter()));
            variables.put("status", event.getStatus());
            variables.put("description", event.getDescription());
            variables.put("transactionDate", event.getTransactionDate().format(dateFormatter));
            
            // Note: We don't have customer email in TransactionEvent
            // So we'll just log it (or fetch from database if needed)
            logger.info("✅ Transaction processed: {} - Amount: {}", 
                    event.getTransactionId(), currencyFormatter.format(event.getAmount()));
            
        } catch (Exception e) {
            logger.error("❌ Error processing TransactionEvent: {}", e.getMessage(), e);
        }
    }
}
