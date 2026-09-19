package com.tushar.hackathon.model.response.dashboard;

import java.util.Map;

/** Headline counts for the dashboard tiles and the risk heatmap. */
public record DashboardSummary(
        long totalCustomers,
        long totalAccounts,
        long totalTransactions,
        long totalAlerts,
        long openAlerts,
        Map<String, Long> alertsBySeverity,
        Map<String, Long> alertsByRule,
        Map<String, Long> alertsByStatus) {
}
