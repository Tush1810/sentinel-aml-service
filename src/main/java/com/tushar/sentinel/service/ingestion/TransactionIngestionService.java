package com.tushar.sentinel.service.ingestion;

import com.tushar.sentinel.exception.ResourceNotFoundException;
import com.tushar.sentinel.exception.ValidationException;
import com.tushar.sentinel.model.response.ingestion.BatchResult;
import com.tushar.sentinel.model.response.ingestion.IngestResult;
import com.tushar.sentinel.model.response.ingestion.RowError;
import com.tushar.sentinel.repository.account.Account;
import com.tushar.sentinel.repository.account.AccountRepository;
import com.tushar.sentinel.repository.txn.Transaction;
import com.tushar.sentinel.repository.txn.TransactionRepository;
import com.tushar.sentinel.repository.txn.TxnDirection;
import com.tushar.sentinel.repository.txn.TxnType;
import com.tushar.sentinel.service.ExchangeRateService;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single place where a transaction enters Sentinel, whatever the transport. The REST adapter
 * and the CSV importer both hand over an {@link IngestTransactionCommand}; a Kafka listener
 * would call the same method.
 */
@Service
public class TransactionIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionIngestionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;

    public TransactionIngestionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            ExchangeRateService exchangeRateService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.exchangeRateService = exchangeRateService;
    }

    /** Incremental path: one transaction, rejected outright if invalid. */
    @Transactional
    public IngestResult ingestOne(IngestTransactionCommand command) {
        ingest(command);
        log.debug("Ingested transaction {} on account {}", command.txnRef(), command.accountRef());
        return new IngestResult(command.txnRef(), "ACCEPTED");
    }

    /** Bulk path: best-effort, so one bad record cannot block the rest of the payload. */
    @Transactional
    public BatchResult ingestBatch(List<IngestTransactionCommand> commands) {
        long startedAt = System.currentTimeMillis();
        String batchId = "ING-TRANSACTION-" + UUID.randomUUID().toString().substring(0, 8);
        List<RowError> errors = new ArrayList<>();
        int accepted = 0;

        for (int i = 0; i < commands.size(); i++) {
            IngestTransactionCommand command = commands.get(i);
            try {
                ingest(command);
                accepted++;
            } catch (RuntimeException e) {
                errors.add(new RowError(i + 1, command.txnRef(), null, e.getMessage()));
            }
        }

        log.info("Ingested transactions batch {}; accepted={} rejected={}", batchId, accepted, errors.size());
        return new BatchResult(batchId, "TRANSACTION", commands.size(), accepted, errors.size(),
                System.currentTimeMillis() - startedAt, errors);
    }

    @Transactional
    public BatchResult ingestCsv(InputStream inputStream) {
        CsvFile csv = new CsvFile(inputStream);
        BatchResult result = csv.load("TRANSACTION", "txn_id", row -> ingest(toCommand(csv, row)));

        log.info("Ingested transactions batch {}; accepted={} rejected={}",
                result.batchId(), result.accepted(), result.rejected());
        return result;
    }

    private void ingest(IngestTransactionCommand command) {
        transactionRepository.save(toTransaction(command));
    }

    private Transaction toTransaction(IngestTransactionCommand command) {
        if (transactionRepository.existsByTxnRef(command.txnRef())) {
            throw new ValidationException("Transaction " + command.txnRef() + " already ingested");
        }
        Account account = accountRepository.findByAccountRef(command.accountRef())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account " + command.accountRef() + " does not exist"));

        Transaction transaction = new Transaction();
        transaction.setTxnRef(command.txnRef());
        transaction.setAccount(account);
        transaction.setDirection(command.direction());
        transaction.setTxnType(command.txnType());
        transaction.setAmount(command.amount());
        transaction.setCurrency(command.currency().toUpperCase());
        transaction.setAmountBase(exchangeRateService.toBaseCurrency(command.amount(), command.currency()));
        transaction.setExchangeRate(exchangeRateService.rateFor(command.currency()));
        transaction.setCounterpartyName(command.counterpartyName());
        transaction.setCounterpartyAccount(command.counterpartyAccount());
        transaction.setCounterpartyBank(command.counterpartyBank());
        transaction.setCounterpartyCountry(command.counterpartyCountry());
        transaction.setChannel(command.channel());
        transaction.setDescription(command.description());
        transaction.setTxnTimestamp(command.txnTimestamp());
        return transaction;
    }

    private IngestTransactionCommand toCommand(CsvFile csv, String[] row) {
        return new IngestTransactionCommand(
                csv.get(row, "txn_id"),
                csv.get(row, "account_id"),
                TxnDirection.valueOf(csv.get(row, "direction")),
                TxnType.valueOf(csv.get(row, "txn_type")),
                CsvValues.toDecimal(csv.get(row, "amount")),
                csv.get(row, "currency"),
                csv.get(row, "counterparty_name"),
                csv.get(row, "counterparty_account"),
                csv.get(row, "counterparty_bank"),
                csv.get(row, "counterparty_country"),
                csv.get(row, "channel"),
                csv.get(row, "description"),
                parseTimestamp(csv.get(row, "txn_timestamp")));
    }

    /** Accepts an ISO instant, a local date-time, or a plain date, in that order. */
    private Instant parseTimestamp(String value) {
        if (value == null) {
            throw new ValidationException("Missing txn_timestamp");
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            try {
                return LocalDateTime.parse(value.replace(" ", "T")).toInstant(ZoneOffset.UTC);
            } catch (RuntimeException alsoIgnored) {
                return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        }
    }
}
