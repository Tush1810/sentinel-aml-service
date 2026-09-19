package com.tushar.sentinel.model.response.ingestion;

/** One rejected record, with enough context for the uploader to fix the source file. */
public record RowError(int row, String reference, String field, String reason) {
}
