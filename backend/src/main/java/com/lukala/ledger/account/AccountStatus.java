package com.lukala.ledger.account;

public enum AccountStatus {
    /** Account can be posted to. */
    ACTIVE,
    /** Temporarily blocked from new postings; history preserved. */
    FROZEN,
    /** Permanently closed; no further postings allowed. */
    CLOSED
}
