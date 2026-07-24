package com.lukala.ledger.banking.connector;

import com.lukala.ledger.banking.BankTransferStatus;

/**
 * Port to an external banking rail (ACH originator, wire gateway, a bank API).
 * The domain depends only on this interface so real banks can be added as
 * adapters without touching the ledger core — mirrors the payment
 * {@link com.lukala.ledger.payment.provider.PaymentProvider} abstraction.
 * See {@link MockBankConnector} for the default sandbox adapter.
 */
public interface BankConnector {

    /** A stable identifier for this connector, e.g. "mock", "acme-bank". */
    String name();

    /**
     * Submit a transfer to the rail. Transfers settle asynchronously, so this
     * typically returns {@link BankTransferStatus#PENDING}; the terminal state
     * arrives later via a settlement callback. Must be safe to retry on
     * {@link BankTransferCommand#reference()}.
     */
    BankTransferResult initiateTransfer(BankTransferCommand command);

    /** Poll the rail for a transfer's current status (reconciliation fallback). */
    BankTransferStatus getTransferStatus(String providerRef);
}
