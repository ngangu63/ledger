package com.lukala.ledger.banking;

/** Lifecycle of an asynchronously-settling bank transfer. */
public enum BankTransferStatus {
    /** Submitted to the rail, not yet settled. */
    PENDING,
    /** Confirmed settled by the rail. */
    SETTLED,
    /** Rejected/returned after submission (e.g. ACH return); reversed in the ledger. */
    RETURNED
}
