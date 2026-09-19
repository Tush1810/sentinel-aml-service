package com.tushar.hackathon.resource.ingestion.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tushar.hackathon.service.ingestion.IngestTransactionCommand;
import com.tushar.hackathon.service.ingestion.TransactionIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound Kafka adapter. Deserialises the message and hands it to the same ingestion
 * service the REST and CSV adapters use; it makes no ingestion decisions of its own.
 */
@Component
@ConditionalOnProperty(name = "sentinel.kafka.enabled", havingValue = "true")
public class TransactionKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionKafkaListener.class);

    private final TransactionIngestionService transactionIngestionService;
    private final ObjectMapper objectMapper;

    public TransactionKafkaListener(
            TransactionIngestionService transactionIngestionService, ObjectMapper objectMapper) {
        this.transactionIngestionService = transactionIngestionService;
        this.objectMapper = objectMapper;
    }

    /** A bad record is logged and skipped; there is no dead-letter topic in this prototype. */
    @KafkaListener(topics = "${sentinel.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onTransaction(String payload) {
        try {
            IngestTransactionCommand command = objectMapper.readValue(payload, IngestTransactionCommand.class);
            transactionIngestionService.ingestOne(command);
            log.info("Ingested transaction {} from Kafka", command.txnRef());
        } catch (Exception e) {
            log.error("Skipping unprocessable Kafka record", e);
        }
    }
}
