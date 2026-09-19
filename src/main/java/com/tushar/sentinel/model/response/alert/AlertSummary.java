package com.tushar.sentinel.model.response.alert;

import com.tushar.sentinel.repository.alert.Alert;

/** Compact alert shape returned alongside an ingestion result. */
public record AlertSummary(String alertRef, String ruleCode, String typology, int riskScore, String severity) {

    public static AlertSummary from(Alert alert) {
        return new AlertSummary(alert.getAlertRef(), alert.getRuleCode(), alert.getTypology(),
                alert.getRiskScore(), alert.getSeverity().name());
    }
}
