package com.lukala.ledger.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. currency mismatch, illegal state transition, insufficient funds).
 */
public class BusinessRuleException extends LedgerException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", message);
    }

    public BusinessRuleException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
