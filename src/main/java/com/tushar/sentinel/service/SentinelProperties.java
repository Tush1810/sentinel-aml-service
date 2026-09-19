package com.tushar.sentinel.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All tunable configuration: currency conversion and the rule book. */
@ConfigurationProperties(prefix = "sentinel")
public record SentinelProperties(Currency currency, Security security, List<Rule> rules) {

    /** Credentials come from the environment; nothing is hardcoded in source. */
    public record Security(String analystPassword, String adminPassword) {
    }

    /** Base currency every amount is converted to, and the rates used to get there. */
    public record Currency(String base, Map<String, BigDecimal> rates) {
    }

    /**
     * One detection rule, entirely from YAML. Adding a rule is a new list entry:
     * no Java change, no redeployment.
     *
     * <p>{@code condition} and {@code filter} are SpEL expressions evaluated against a
     * transaction, so its properties are referenced by name (for example {@code amountBase}).
     */
    public record Rule(
            String code,
            String typology,
            boolean enabled,
            int weight,
            Type type,
            Scope scope,
            String condition,
            String filter,
            BigDecimal threshold,
            int windowHours,
            int baselineDays,
            int minBaselineTxns,
            String explanation) {
    }

    /** Shapes of detection logic. A new rule reuses one; only its parameters differ. */
    public enum Type {
        SINGLE_TRANSACTION,
        WINDOWED_COUNT,
        INFLOW_OUTFLOW_RATIO,
        BASELINE_DEVIATION
    }

    public enum Scope {
        ACCOUNT,
        CUSTOMER
    }
}
