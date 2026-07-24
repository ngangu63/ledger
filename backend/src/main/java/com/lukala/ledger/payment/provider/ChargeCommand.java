package com.lukala.ledger.payment.provider;

import com.lukala.ledger.common.money.Money;
import com.lukala.ledger.payment.PaymentDirection;
import com.lukala.ledger.payment.PaymentMethod;
import java.util.UUID;

/**
 * A vendor-neutral instruction to move money through an external rail.
 */
public record ChargeCommand(
        UUID paymentId,
        PaymentDirection direction,
        PaymentMethod method,
        Money amount,
        String instrumentToken,
        String reference) {
}
