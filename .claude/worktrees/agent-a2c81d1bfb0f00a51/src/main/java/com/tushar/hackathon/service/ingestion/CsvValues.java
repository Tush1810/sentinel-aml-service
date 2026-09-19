package com.tushar.hackathon.service.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Conversions for the loose value formats the source exports use. */
public final class CsvValues {

    private static final Set<String> TRUE_VALUES = Set.of("Y", "YES", "TRUE", "1");

    private CsvValues() {
    }

    public static boolean toBoolean(String value) {
        return value != null && TRUE_VALUES.contains(value.toUpperCase());
    }

    public static Integer toInteger(String value) {
        return value == null ? null : Integer.valueOf((int) Double.parseDouble(value));
    }

    public static int toIntOrZero(String value) {
        Integer parsed = toInteger(value);
        return parsed == null ? 0 : parsed;
    }

    public static BigDecimal toDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    public static LocalDate toDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
