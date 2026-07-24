package com.lukala.ledger.banking.connector;

import com.lukala.ledger.banking.BankRail;
import com.lukala.ledger.common.money.Money;
import java.util.UUID;

/**
 * A vendor-neutral instruction to move money over a bank rail. The domain builds
 * this and hands it to a {@link BankConnector}; it never depends on a bank SDK.
 */
public record BankTransferCommand(
        UUID transferId,
        BankRail rail,
        Money amount,
        String reference) {
}
