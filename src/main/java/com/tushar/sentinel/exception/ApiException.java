package com.tushar.sentinel.exception;

/**
 * Base for failures the API reports deliberately. Subclasses override {@link #getErrorCode()},
 * so the exception handler reads the code off the exception instead of switching on its type.
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ErrorCode getErrorCode() {
        return ErrorCode.INTERNAL_ERROR;
    }
}
