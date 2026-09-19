package com.tushar.sentinel.model.response.ingestion;

import java.util.List;

/**
 * Outcome of a bulk load. Ingestion is best-effort: valid rows are accepted and the
 * rejected ones are reported, because partial monitoring beats none.
 */
public record BatchResult(
        String batchId,
        String entity,
        int received,
        int accepted,
        int rejected,
        long durationMs,
        List<RowError> errors) {
}
