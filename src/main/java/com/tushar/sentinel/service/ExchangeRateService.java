package com.tushar.sentinel.service;

import com.tushar.sentinel.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Normalizes amounts to the base currency at ingestion, so detection rules compare like
 * with like and never deal with currency conversion themselves.
 */
@Service
public class ExchangeRateService {

    private static final int BASE_SCALE = 2;

    private final SentinelProperties properties;

    public ExchangeRateService(SentinelProperties properties) {
        this.properties = properties;
    }

    public BigDecimal rateFor(String currency) {
        BigDecimal rate = properties.currency().rates().get(currency.toUpperCase(Locale.ROOT));
        if (rate == null) {
            throw new ValidationException("No exchange rate configured for currency " + currency);
        }
        return rate;
    }

    public BigDecimal toBaseCurrency(BigDecimal amount, String currency) {
        return amount.multiply(rateFor(currency)).setScale(BASE_SCALE, RoundingMode.HALF_UP);
    }
}
