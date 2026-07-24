package com.lukala.ledger.account.dto;

import com.lukala.ledger.account.Account;
import com.lukala.ledger.account.AccountStatus;
import com.lukala.ledger.account.AccountType;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String currency,
        AccountStatus status,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt());
    }
}
