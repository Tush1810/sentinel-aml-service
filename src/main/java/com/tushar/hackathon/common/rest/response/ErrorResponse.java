package com.tushar.hackathon.common.rest.response;

import java.time.Instant;
import java.util.List;

/**
 * Error shape returned by every failing endpoint. {@code errorCode} is null for framework
 * errors such as an unknown path, which carry a status but no domain code.
 */
public record ErrorResponse(Integer errorCode, String message, List<String> details, Instant timestamp) {

    public static ErrorResponse of(int errorCode, String message) {
        return new ErrorResponse(errorCode, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(int errorCode, String message, List<String> details) {
        return new ErrorResponse(errorCode, message, details, Instant.now());
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(null, message, List.of(), Instant.now());
    }
}
