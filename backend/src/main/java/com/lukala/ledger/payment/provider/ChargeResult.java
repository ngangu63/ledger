package com.lukala.ledger.payment.provider;

/**
 * The outcome of a provider charge. {@code providerRef} is the external system's
 * identifier, retained for reconciliation and refunds.
 */
public record ChargeResult(boolean approved, String providerRef, String failureReason) {

    public static ChargeResult approved(String providerRef) {
        return new ChargeResult(true, providerRef, null);
    }

    public static ChargeResult declined(String reason) {
        return new ChargeResult(false, null, reason);
    }
}
