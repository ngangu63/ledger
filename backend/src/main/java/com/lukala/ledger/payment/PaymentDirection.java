package com.lukala.ledger.payment;

public enum PaymentDirection {
    /** Money coming in (e.g. a customer card charge). */
    INBOUND,
    /** Money going out (e.g. a payout or reimbursement). */
    OUTBOUND
}
