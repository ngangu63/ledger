package com.lukala.ledger.payment.provider;

/**
 * Port to an external payment rail (card processor, bank ACH, etc.). The domain
 * depends only on this interface — never a vendor SDK — so real providers
 * (Stripe, Adyen, a bank API) can be added as adapters without touching the
 * ledger core. See {@link MockPaymentProvider} for the default sandbox adapter.
 */
public interface PaymentProvider {

    /** A stable identifier for this provider, e.g. "mock", "stripe". */
    String name();

    /**
     * Authorize and capture a charge in one step. Implementations must be safe to
     * retry: passing the same {@link ChargeCommand#reference()} should not move
     * money twice.
     */
    ChargeResult charge(ChargeCommand command);

    /** Reverse/refund a previously approved charge. */
    ChargeResult refund(String providerRef);
}
