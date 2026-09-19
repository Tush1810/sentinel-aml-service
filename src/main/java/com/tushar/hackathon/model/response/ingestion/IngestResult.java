package com.tushar.hackathon.model.response.ingestion;

import com.tushar.hackathon.model.response.alert.AlertSummary;
import java.util.List;

/** Outcome of a single incremental ingestion, including anything detection raised. */
public record IngestResult(String txnRef, String status, List<AlertSummary> alerts) {
}
