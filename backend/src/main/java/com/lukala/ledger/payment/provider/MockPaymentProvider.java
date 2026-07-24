package com.lukala.ledger.payment.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Sandbox adapter that approves charges deterministically without contacting any
 * external system. To exercise the failure path in tests, set the charge
 * {@code instrumentToken} to {@code "decline"}.
 *
 * <p>Swap this for a real adapter by implementing {@link PaymentProvider} and
 * setting {@code ledger.payment.provider=stripe} (etc.); this bean is the default.
 */
@Component
@ConditionalOnProperty(name = "ledger.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public ChargeResult charge(ChargeCommand command) {
        if ("decline".equalsIgnoreCase(command.instrumentToken())) {
            return ChargeResult.declined("Card declined (mock).");
        }
        return ChargeResult.approved("mock_ch_" + command.paymentId());
    }

    @Override
    public ChargeResult refund(String providerRef) {
        return ChargeResult.approved("mock_rf_" + providerRef);
    }
}
