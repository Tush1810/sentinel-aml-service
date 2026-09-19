package com.tushar.sentinel.model.response.ingestion;

/**
 * Outcome of a single incremental ingestion. Detection now runs asynchronously in a separate
 * service, so this response only confirms the transaction was accepted; it cannot report alerts.
 */
public record IngestResult(String txnRef, String status) {
}
