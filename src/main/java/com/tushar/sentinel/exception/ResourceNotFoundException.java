package com.tushar.sentinel.exception;

/** Requested entity does not exist. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.ENTITY_NOT_FOUND;
    }
}
