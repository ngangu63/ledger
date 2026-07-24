package com.lukala.ledger.cash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request to remove cash from an account. The currency is taken from the target
 * account (accounts are single-currency), so it is not part of the request.
 */
public record WithdrawRequest(
        @NotNull
        @DecimalMin(value = "0.0001", message = "amount must be positive")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,
        @Size(max = 1024) String description) {
}
