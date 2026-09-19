package com.tushar.hackathon.service.fx;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configurable exchange rate table; changing a rate needs no redeployment. */
@ConfigurationProperties(prefix = "sentinel.fx")
public record ExchangeRateProperties(String baseCurrency, Map<String, BigDecimal> rates) {
}
