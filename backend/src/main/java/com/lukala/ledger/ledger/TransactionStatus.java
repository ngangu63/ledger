package com.lukala.ledger.ledger;

public enum TransactionStatus {
    /** Successfully posted and balanced. Immutable. */
    POSTED,
    /** A reversing transaction that offsets an earlier one. */
    REVERSED
}
