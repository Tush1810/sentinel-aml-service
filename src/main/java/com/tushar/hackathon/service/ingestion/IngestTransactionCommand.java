package com.tushar.hackathon.service.ingestion;

import com.tushar.hackathon.repository.txn.TxnDirection;
import com.tushar.hackathon.repository.txn.TxnType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Transport-neutral ingestion input. The REST adapter and the CSV importer both build this,
 * so the ingestion service never learns which one delivered the record.
 */
public record IngestTransactionCommand(
        @NotBlank String txnRef,
        @NotBlank String accountRef,
        @NotNull TxnDirection direction,
        @NotNull TxnType txnType,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        String counterpartyName,
        String counterpartyAccount,
        String counterpartyBank,
        String counterpartyCountry,
        String channel,
        String description,
        @NotNull Instant txnTimestamp) {
}
