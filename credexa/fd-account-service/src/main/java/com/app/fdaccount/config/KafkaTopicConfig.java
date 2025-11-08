package com.app.fdaccount.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topics Configuration
 * Creates Kafka topics if they don't exist
 * Only active when kafka.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

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

    @Bean
    public NewTopic accountCreatedTopic() {
        return TopicBuilder.name(accountCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountClosedTopic() {
        return TopicBuilder.name(accountClosedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name(transactionCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic maturityProcessedTopic() {
        return TopicBuilder.name(maturityProcessedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic interestAccruedTopic() {
        return TopicBuilder.name(interestAccruedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic withdrawalProcessedTopic() {
        return TopicBuilder.name(withdrawalProcessedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
