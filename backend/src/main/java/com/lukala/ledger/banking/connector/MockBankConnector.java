package com.lukala.ledger.banking.connector;

import com.lukala.ledger.banking.BankTransferStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sandbox adapter that accepts transfers deterministically without contacting any
 * bank. Transfers come back {@link BankTransferStatus#PENDING}; settlement is
 * driven in tests/dev by posting a callback to the banking webhook endpoint.
 *
 * <p>Swap this for a real adapter by implementing {@link BankConnector} and
 * setting {@code ledger.bank.connector=<name>}; this bean is the default.
 */
@Component
@ConditionalOnProperty(name = "ledger.bank.connector", havingValue = "mock", matchIfMissing = true)
public class MockBankConnector implements BankConnector {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public BankTransferResult initiateTransfer(BankTransferCommand command) {
        return new BankTransferResult("mock_" + command.rail().name().toLowerCase() + "_"
                + command.transferId(), BankTransferStatus.PENDING);
    }

    @Override
    public BankTransferStatus getTransferStatus(String providerRef) {
        return BankTransferStatus.SETTLED;
    }
}
