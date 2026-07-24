package com.lukala.ledger.ledger.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request to post a balanced transaction. The {@code currency} applies to every
 * posting — a single transaction is single-currency by design.
 */
public record CreateTransactionRequest(
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter ISO-4217 code")
        String currency,
        @Size(max = 255) String externalRef,
        @Size(max = 1024) String description,
        @NotEmpty @Size(min = 2, message = "a transaction needs at least two postings")
        @Valid List<PostingRequest> postings) {
}
