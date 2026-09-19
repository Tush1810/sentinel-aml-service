package com.tushar.sentinel.service;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** All tunable configuration: currency conversion and credentials. */
@ConfigurationProperties(prefix = "sentinel")
public record SentinelProperties(Currency currency, Security security) {

    /** Credentials come from the environment; nothing is hardcoded in source. */
    public record Security(String analystPassword, String adminPassword) {
    }

    /** Base currency every amount is converted to, and the rates used to get there. */
    public record Currency(String base, Map<String, BigDecimal> rates) {
    }
}
