package com.lukala.ledger.ledger.dto;

import com.lukala.ledger.ledger.LedgerTransaction;
import com.lukala.ledger.ledger.TransactionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String externalRef,
        String description,
        TransactionStatus status,
        UUID reversalOfId,
        Instant createdAt,
        List<PostingResponse> postings) {

    public static TransactionResponse from(LedgerTransaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getExternalRef(),
                tx.getDescription(),
                tx.getStatus(),
                tx.getReversalOfId(),
                tx.getCreatedAt(),
                tx.getPostings().stream().map(PostingResponse::from).toList());
    }
}
