package com.tushar.hackathon.resource.ingestion.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the ingestion topic. Messages are keyed by account reference, so every
 * transaction for one account lands on one partition and is consumed in order.
 */
@Configuration
@ConditionalOnProperty(name = "sentinel.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    @Bean
    public NewTopic transactionsTopic(@Value("${sentinel.kafka.topic}") String topic) {
        return new NewTopic(topic, PARTITIONS, REPLICAS);
    }
}
