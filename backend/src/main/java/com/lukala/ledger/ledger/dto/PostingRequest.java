package com.lukala.ledger.ledger.dto;

import com.lukala.ledger.ledger.PostingDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record PostingRequest(
        @NotNull UUID accountId,
        @NotNull PostingDirection direction,
        @NotNull
        @DecimalMin(value = "0.0001", message = "amount must be positive")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount) {
}
