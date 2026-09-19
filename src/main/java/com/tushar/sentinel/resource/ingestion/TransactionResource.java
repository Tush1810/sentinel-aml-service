package com.tushar.sentinel.resource.ingestion;

import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.IngestResult;
import com.tushar.sentinel.service.ingestion.IngestTransactionCommand;
import com.tushar.sentinel.service.ingestion.TransactionIngestionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Incremental ingestion adapter for transactions arriving continuously. */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionResource {

    private final TransactionIngestionService transactionIngestionService;

    public TransactionResource(TransactionIngestionService transactionIngestionService) {
        this.transactionIngestionService = transactionIngestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngestResult ingest(@Valid @RequestBody IngestTransactionCommand command) {
        return transactionIngestionService.ingestOne(command);
    }

    @PostMapping("/batch")
    public BatchResult ingestBatch(@Valid @RequestBody List<IngestTransactionCommand> commands) {
        return transactionIngestionService.ingestBatch(commands);
    }
}
