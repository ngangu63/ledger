package com.lukala.ledger.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Account balance. {@code signedBalance} is debits minus credits;
 * {@code naturalBalance} applies the account type's normal side so it reads
 * positive for a healthy account (e.g. a positive asset, a positive liability).
 */
public record BalanceResponse(
        UUID accountId,
        String currency,
        BigDecimal signedBalance,
        BigDecimal naturalBalance) {
}
