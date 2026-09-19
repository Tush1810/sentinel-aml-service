package com.tushar.sentinel.exception;

/** Input is well-formed but breaks a domain rule. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.VALIDATION_FAILED;
    }
}
