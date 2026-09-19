package com.tushar.sentinel.model.response.dashboard;

import java.util.List;

/** One row of the customer list, with the counts an analyst scans for. */
public record CustomerView(
        String customerRef,
        String name,
        String city,
        String segment,
        String kycStatus,
        String riskRating,
        boolean politicallyExposed,
        List<String> accountRefs,
        long transactions,
        long alerts,
        int highestRiskScore) {
}
