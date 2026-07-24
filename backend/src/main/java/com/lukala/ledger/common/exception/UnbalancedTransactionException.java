package com.lukala.ledger.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a transaction's debits do not equal its credits per currency —
 * the cardinal sin of double-entry accounting.
 */
public class UnbalancedTransactionException extends LedgerException {

    public UnbalancedTransactionException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSACTION_UNBALANCED", message);
    }
}
