package com.lukala.ledger.banking.dto;

import com.lukala.ledger.banking.BankRail;
import com.lukala.ledger.banking.BankTransfer;
import com.lukala.ledger.banking.BankTransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BankTransferResponse(
        UUID id,
        BankRail rail,
        BigDecimal amount,
        String currency,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BankTransferStatus status,
        String provider,
        String providerRef,
        String externalRef,
        UUID transactionId,
        Instant settledAt,
        Instant createdAt) {

    public static BankTransferResponse from(BankTransfer t) {
        return new BankTransferResponse(
                t.getId(), t.getRail(), t.getMoney().getAmount(), t.getMoney().getCurrency(),
                t.getSourceAccountId(), t.getDestinationAccountId(), t.getStatus(),
                t.getProvider(), t.getProviderRef(), t.getExternalRef(),
                t.getTransactionId(), t.getSettledAt(), t.getCreatedAt());
    }
}
