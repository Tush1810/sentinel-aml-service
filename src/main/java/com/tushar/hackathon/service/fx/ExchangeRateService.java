package com.tushar.hackathon.service.fx;

import com.tushar.hackathon.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Normalizes every amount to the base currency so threshold rules compare like with like.
 * Conversion happens at ingestion, which keeps the detection rules free of FX concerns.
 */
@Service
public class ExchangeRateService {

    private static final int BASE_SCALE = 2;

    private final ExchangeRateProperties properties;

    public ExchangeRateService(ExchangeRateProperties properties) {
        this.properties = properties;
    }

    public String baseCurrency() {
        return properties.baseCurrency();
    }

    public BigDecimal rateFor(String currency) {
        BigDecimal rate = properties.rates().get(currency.toUpperCase(Locale.ROOT));
        if (rate == null) {
            throw new ValidationException("No exchange rate configured for currency " + currency);
        }
        return rate;
    }

    public BigDecimal toBaseCurrency(BigDecimal amount, String currency) {
        return amount.multiply(rateFor(currency)).setScale(BASE_SCALE, RoundingMode.HALF_UP);
    }
}
