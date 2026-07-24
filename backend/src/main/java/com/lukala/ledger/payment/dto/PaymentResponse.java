package com.lukala.ledger.payment.dto;

import com.lukala.ledger.payment.Payment;
import com.lukala.ledger.payment.PaymentDirection;
import com.lukala.ledger.payment.PaymentMethod;
import com.lukala.ledger.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentDirection direction,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String provider,
        String providerRef,
        String externalRef,
        UUID transactionId,
        String failureReason,
        Instant createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getDirection(), p.getMethod(), p.getStatus(),
                p.getMoney().getAmount(), p.getMoney().getCurrency(),
                p.getSourceAccountId(), p.getDestinationAccountId(),
                p.getProvider(), p.getProviderRef(), p.getExternalRef(),
                p.getTransactionId(), p.getFailureReason(), p.getCreatedAt());
    }
}
