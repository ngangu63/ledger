package com.lukala.ledger.banking;

/** The bank rail a transfer moves over. */
public enum BankRail {
    /** Automated Clearing House — batched, slow (settles in days), returnable. */
    ACH,
    /** Wire transfer — near-real-time, generally irrevocable. */
    WIRE
}
