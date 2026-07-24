package com.lukala.ledger.banking.dto;

import com.lukala.ledger.banking.BankTransferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * A settlement callback from a bank rail, correlated to a transfer by
 * {@code providerRef}. Only terminal states (SETTLED / RETURNED) are actionable.
 *
 * <p>In production a webhook like this must be authenticated by verifying a
 * provider signature (HMAC) rather than a bearer token; that is out of scope for
 * the sandbox connector.
 */
public record SettlementEvent(
        @NotBlank String providerRef,
        @NotNull BankTransferStatus status) {
}
