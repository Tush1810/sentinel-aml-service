package com.tushar.sentinel.resource.ingestion;

import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.service.ingestion.AccountIngestionService;
import com.tushar.sentinel.service.ingestion.CustomerIngestionService;
import com.tushar.sentinel.service.ingestion.TransactionIngestionService;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Bulk load adapter. Translates an upload into the transport-neutral ingestion services. */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionResource {

    private final CustomerIngestionService customerIngestionService;
    private final AccountIngestionService accountIngestionService;
    private final TransactionIngestionService transactionIngestionService;

    public IngestionResource(
            CustomerIngestionService customerIngestionService,
            AccountIngestionService accountIngestionService,
            TransactionIngestionService transactionIngestionService) {
        this.customerIngestionService = customerIngestionService;
        this.accountIngestionService = accountIngestionService;
        this.transactionIngestionService = transactionIngestionService;
    }

    @PostMapping(value = "/customers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchResult ingestCustomers(@RequestParam("file") MultipartFile file) {
        return customerIngestionService.ingestCsv(streamOf(file));
    }

    @PostMapping(value = "/accounts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchResult ingestAccounts(@RequestParam("file") MultipartFile file) {
        return accountIngestionService.ingestCsv(streamOf(file));
    }

    @PostMapping(value = "/transactions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchResult ingestTransactions(@RequestParam("file") MultipartFile file) {
        return transactionIngestionService.ingestCsv(streamOf(file));
    }

    private InputStream streamOf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("Uploaded file is empty");
        }
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new ValidationException("Could not read uploaded file: " + e.getMessage());
        }
    }
}
