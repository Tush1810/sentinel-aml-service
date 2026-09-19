package com.tushar.hackathon.model.response.ingestion;

/** Outcome of a single incremental ingestion. */
public record IngestResult(String txnRef, String status, Long transactionId) {

    public static IngestResult accepted(String txnRef, Long transactionId) {
        return new IngestResult(txnRef, "ACCEPTED", transactionId);
    }
}
