package com.lukala.ledger.ledger;

import com.lukala.ledger.common.idempotency.IdempotencyService;
import com.lukala.ledger.ledger.dto.BalanceResponse;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ledger", description = "Double-entry transactions and balances")
public class LedgerController {

    private static final String TX_SCOPE = "ledger.transaction.create";
    private static final String REVERSAL_SCOPE = "ledger.transaction.reverse";

    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;

    public LedgerController(LedgerService ledgerService, IdempotencyService idempotencyService) {
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/transactions")
    @Operation(summary = "Post a balanced double-entry transaction")
    public ResponseEntity<TransactionResponse> post(
            @Parameter(description = "Idempotency-Key; retries with the same key and body return the original result")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request) {

        UUID txId = idempotencyService.execute(idempotencyKey, TX_SCOPE, request.toString(),
                () -> ledgerService.post(request));
        TransactionResponse body = TransactionResponse.from(ledgerService.getTransaction(txId));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + txId)).body(body);
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get a transaction with its postings")
    public TransactionResponse get(@PathVariable UUID id) {
        return TransactionResponse.from(ledgerService.getTransaction(id));
    }

    @PostMapping("/transactions/{id}/reversal")
    @Operation(summary = "Reverse a transaction by posting an offsetting entry")
    public ResponseEntity<TransactionResponse> reverse(
            @Parameter(description = "Idempotency-Key; retries with the same key return the original reversal")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable UUID id,
            @RequestParam(required = false) String description) {
        UUID reversalId = idempotencyService.execute(idempotencyKey, REVERSAL_SCOPE, "reverse:" + id,
                () -> ledgerService.reverse(id, description));
        TransactionResponse body = TransactionResponse.from(ledgerService.getTransaction(reversalId));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + reversalId)).body(body);
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "Get the current balance of an account")
    public BalanceResponse balance(@PathVariable UUID accountId) {
        return ledgerService.getBalance(accountId);
    }
}
