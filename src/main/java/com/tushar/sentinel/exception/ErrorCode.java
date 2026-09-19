package com.tushar.sentinel.exception;

/**
 * Error codes are sub-codes of the HTTP status: the status is the first three digits,
 * so 4041 maps to 404 and 5001 to 500. Clients get a precise code, the status can never drift.
 */
public enum ErrorCode {
    BAD_REQUEST(4001),
    VALIDATION_FAILED(4002),
    ENTITY_NOT_FOUND(4041),
    INTERNAL_ERROR(5001);

    private static final int MIN_CODE = 1000;
    private static final int MAX_CODE = 9999;

    private final int code;

    ErrorCode(int code) {
        if (code < MIN_CODE || code > MAX_CODE) {
            throw new IllegalArgumentException("Error code must be between " + MIN_CODE + " and " + MAX_CODE);
        }
        this.code = code;
    }

    public int code() {
        return code;
    }

    public int toHttpStatusCode() {
        return code / 10;
    }
}
