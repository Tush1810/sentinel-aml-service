package com.tushar.sentinel.resource.ingestion.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.service.ingestion.IngestTransactionCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publishes an ingestion command onto the topic, keyed by account for per-account ordering. */
@Component
@ConditionalOnProperty(name = "sentinel.kafka.enabled", havingValue = "true")
public class TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public TransactionEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${sentinel.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(IngestTransactionCommand command) {
        try {
            kafkaTemplate.send(topic, command.accountRef(), objectMapper.writeValueAsString(command));
            log.debug("Published transaction {} to {}", command.txnRef(), topic);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Could not serialise transaction " + command.txnRef());
        }
    }
}
