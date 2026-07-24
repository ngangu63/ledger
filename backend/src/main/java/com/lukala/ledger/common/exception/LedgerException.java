package com.lukala.ledger.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for domain errors that map to a specific HTTP status.
 */
public abstract class LedgerException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected LedgerException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
