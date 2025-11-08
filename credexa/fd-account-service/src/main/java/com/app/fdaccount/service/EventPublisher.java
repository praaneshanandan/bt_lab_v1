package com.app.fdaccount.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.app.fdaccount.event.AccountClosedEvent;
import com.app.fdaccount.event.AccountCreatedEvent;
import com.app.fdaccount.event.InterestAccruedEvent;
import com.app.fdaccount.event.MaturityProcessedEvent;
import com.app.fdaccount.event.TransactionCreatedEvent;
import com.app.fdaccount.event.WithdrawalProcessedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for publishing events to Kafka
 * Handles all event publishing for FD account operations
 * Only active when kafka.enabled=true
 */
@Slf4j
@Service
public class EventPublisher {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${kafka.topics.account-created:account-created-events}")
    private String accountCreatedTopic;

    @Value("${kafka.topics.account-closed:account-closed-events}")
    private String accountClosedTopic;

    @Value("${kafka.topics.transaction-created:transaction-created-events}")
    private String transactionCreatedTopic;

    @Value("${kafka.topics.maturity-processed:maturity-processed-events}")
    private String maturityProcessedTopic;

    @Value("${kafka.topics.interest-accrued:interest-accrued-events}")
    private String interestAccruedTopic;

    @Value("${kafka.topics.withdrawal-processed:withdrawal-processed-events}")
    private String withdrawalProcessedTopic;

    /**
     * Publish account created event
     */
    public void publishAccountCreated(AccountCreatedEvent event) {
        enrichEvent(event);
        publishEvent(accountCreatedTopic, event.getAccountNumber(), event);
    }

    /**
     * Publish account closed event
     */
    public void publishAccountClosed(AccountClosedEvent event) {
        enrichEvent(event);
        publishEvent(accountClosedTopic, event.getAccountNumber(), event);
    }

    /**
     * Publish transaction created event
     */
    public void publishTransactionCreated(TransactionCreatedEvent event) {
        enrichEvent(event);
        publishEvent(transactionCreatedTopic, event.getAccountNumber(), event);
    }

    /**
     * Publish maturity processed event
     */
    public void publishMaturityProcessed(MaturityProcessedEvent event) {
        enrichEvent(event);
        publishEvent(maturityProcessedTopic, event.getAccountNumber(), event);
    }

    /**
     * Publish interest accrued event
     */
    public void publishInterestAccrued(InterestAccruedEvent event) {
        enrichEvent(event);
        publishEvent(interestAccruedTopic, event.getAccountNumber(), event);
    }

    /**
     * Publish withdrawal processed event
     */
    public void publishWithdrawalProcessed(WithdrawalProcessedEvent event) {
        enrichEvent(event);
        publishEvent(withdrawalProcessedTopic, event.getAccountNumber(), event);
    }

    /**
     * Enrich event with common fields
     */
    private void enrichEvent(Object event) {
        try {
            // Use reflection to set eventId and timestamp if not already set
            var eventClass = event.getClass();
            
            var eventIdField = eventClass.getDeclaredField("eventId");
            eventIdField.setAccessible(true);
            if (eventIdField.get(event) == null) {
                eventIdField.set(event, UUID.randomUUID().toString());
            }

            var timestampField = eventClass.getDeclaredField("timestamp");
            timestampField.setAccessible(true);
            if (timestampField.get(event) == null) {
                timestampField.set(event, LocalDateTime.now());
            }

            var eventTypeField = eventClass.getDeclaredField("eventType");
            eventTypeField.setAccessible(true);
            if (eventTypeField.get(event) == null) {
                eventTypeField.set(event, eventClass.getSimpleName());
            }
        } catch (Exception e) {
            log.warn("Failed to enrich event: {}", e.getMessage());
        }
    }

    /**
     * Publish event to Kafka topic
     */
    private void publishEvent(String topic, String key, Object event) {
        // Check if Kafka is enabled
        if (!kafkaEnabled) {
            log.debug("⚠️ Kafka is disabled - Event not published: {} to topic '{}'", 
                    event.getClass().getSimpleName(), topic);
            return;
        }

        // Check if KafkaTemplate is available
        if (kafkaTemplate == null) {
            log.warn("⚠️ KafkaTemplate not available - Event not published: {} to topic '{}'", 
                    event.getClass().getSimpleName(), topic);
            return;
        }

        try {
            log.debug("📤 Publishing event to topic '{}' with key '{}'", topic, key);
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("✅ Event published successfully to topic '{}' - Partition: {}, Offset: {}, Event: {}", 
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            event.getClass().getSimpleName());
                } else {
                    log.error("❌ Failed to publish event to topic '{}': {}", topic, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            log.error("❌ Error publishing event to topic '{}': {}", topic, e.getMessage(), e);
        }
    }
}
