package com.lukala.ledger.banking.connector;

import com.lukala.ledger.banking.BankTransferStatus;

/**
 * The connector's response to an initiation. {@code providerRef} is the rail's
 * identifier, retained so asynchronous settlement callbacks can be correlated
 * back to the transfer.
 */
public record BankTransferResult(String providerRef, BankTransferStatus status) {
}
