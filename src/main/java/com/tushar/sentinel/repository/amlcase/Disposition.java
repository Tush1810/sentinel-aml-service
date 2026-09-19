package com.tushar.sentinel.repository.amlcase;

/** How an investigation was resolved; retained for audit. */
public enum Disposition {
    FALSE_POSITIVE, CLEARED, ESCALATED_TO_SAR
}
