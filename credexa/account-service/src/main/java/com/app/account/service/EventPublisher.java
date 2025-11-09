package com.app.account.service;

import com.app.account.event.AccountCreatedEvent;
import com.app.account.event.AlertEvent;
import com.app.account.event.TransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Simple Kafka event publisher
 * Only activates if kafka.enabled=true
 * Fails gracefully if Kafka is not available
 */
@Service
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.account-created:account-created}")
    private String accountCreatedTopic;

    @Value("${kafka.topics.transaction:account-transaction}")
    private String transactionTopic;

    @Value("${kafka.topics.alert:customer-alert}")
    private String alertTopic;

    /**
     * Publish account created event
     */
    public void publishAccountCreated(AccountCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(accountCreatedTopic, event.getAccountNumber(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("✅ Published AccountCreatedEvent for account: {}", event.getAccountNumber());
                        } else {
                            logger.error("❌ Failed to publish AccountCreatedEvent: {}", ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("❌ Error serializing AccountCreatedEvent: {}", e.getMessage());
        }
    }

    /**
     * Publish transaction event
     */
    public void publishTransaction(TransactionEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(transactionTopic, event.getTransactionId(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("✅ Published TransactionEvent: {} for account: {}", 
                                    event.getTransactionType(), event.getAccountNumber());
                        } else {
                            logger.error("❌ Failed to publish TransactionEvent: {}", ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("❌ Error serializing TransactionEvent: {}", e.getMessage());
        }
    }

    /**
     * Publish alert event
     */
    public void publishAlert(AlertEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(alertTopic, event.getCustomerId().toString(), eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("✅ Published AlertEvent: {} for customer: {}", 
                                    event.getAlertType(), event.getCustomerId());
                        } else {
                            logger.error("❌ Failed to publish AlertEvent: {}", ex.getMessage());
                        }
                    });
        } catch (JsonProcessingException e) {
            logger.error("❌ Error serializing AlertEvent: {}", e.getMessage());
        }
    }
}
