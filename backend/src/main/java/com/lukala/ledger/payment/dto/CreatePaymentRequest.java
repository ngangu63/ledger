package com.lukala.ledger.payment.dto;

import com.lukala.ledger.payment.PaymentDirection;
import com.lukala.ledger.payment.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to charge a payment. On capture, the ledger records a transfer that
 * debits {@code destinationAccountId} and credits {@code sourceAccountId}.
 */
public record CreatePaymentRequest(
        @NotNull PaymentDirection direction,
        @NotNull PaymentMethod method,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        String instrumentToken,
        String externalRef) {
}
