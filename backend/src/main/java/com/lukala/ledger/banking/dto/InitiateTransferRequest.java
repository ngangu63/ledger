package com.lukala.ledger.banking.dto;

import com.lukala.ledger.banking.BankRail;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to initiate a bank transfer. On the ledger it debits
 * {@code destinationAccountId} and credits {@code sourceAccountId}.
 */
public record InitiateTransferRequest(
        @NotNull BankRail rail,
        @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull UUID sourceAccountId,
        @NotNull UUID destinationAccountId,
        String externalRef) {
}
