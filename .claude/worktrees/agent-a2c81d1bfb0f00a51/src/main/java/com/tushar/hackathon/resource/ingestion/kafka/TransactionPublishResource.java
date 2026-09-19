package com.tushar.hackathon.resource.ingestion.kafka;

import com.tushar.hackathon.service.ingestion.IngestTransactionCommand;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asynchronous ingestion door. Validates structurally, publishes, and returns 202 —
 * the transaction is persisted by the listener, so no alert count comes back here.
 */
@RestController
@RequestMapping("/api/v1/ingestion/transactions")
@ConditionalOnProperty(name = "sentinel.kafka.enabled", havingValue = "true")
public class TransactionPublishResource {

    private final TransactionEventPublisher publisher;

    public TransactionPublishResource(TransactionEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> publish(@Valid @RequestBody List<IngestTransactionCommand> commands) {
        commands.forEach(publisher::publish);
        return Map.of("queued", commands.size(), "status", "QUEUED");
    }
}
