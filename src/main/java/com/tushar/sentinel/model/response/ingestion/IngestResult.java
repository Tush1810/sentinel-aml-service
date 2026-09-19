package com.tushar.sentinel.model.response.ingestion;

import com.tushar.sentinel.model.response.alert.AlertSummary;
import java.util.List;

/** Outcome of a single incremental ingestion, including anything detection raised. */
public record IngestResult(String txnRef, String status, List<AlertSummary> alerts) {
}
