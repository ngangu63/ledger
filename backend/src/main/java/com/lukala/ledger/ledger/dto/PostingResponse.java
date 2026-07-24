package com.lukala.ledger.ledger.dto;

import com.lukala.ledger.ledger.Posting;
import com.lukala.ledger.ledger.PostingDirection;
import java.math.BigDecimal;
import java.util.UUID;

public record PostingResponse(
        UUID id,
        UUID accountId,
        PostingDirection direction,
        BigDecimal amount,
        String currency) {

    public static PostingResponse from(Posting p) {
        return new PostingResponse(
                p.getId(),
                p.getAccountId(),
                p.getDirection(),
                p.getMoney().getAmount(),
                p.getMoney().getCurrency());
    }
}
