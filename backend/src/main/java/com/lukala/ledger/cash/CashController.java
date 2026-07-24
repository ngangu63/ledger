package com.lukala.ledger.cash;

import com.lukala.ledger.cash.dto.DepositRequest;
import com.lukala.ledger.cash.dto.WithdrawRequest;
import com.lukala.ledger.common.idempotency.IdempotencyService;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.ledger.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Cash", description = "Add cash to an account (balanced double-entry deposit)")
public class CashController {

    private static final String DEPOSIT_SCOPE = "cash.deposit";
    private static final String WITHDRAW_SCOPE = "cash.withdraw";

    private final CashService cashService;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;

    public CashController(CashService cashService, LedgerService ledgerService,
                          IdempotencyService idempotencyService) {
        this.cashService = cashService;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Add cash to an account, recorded as a balanced double-entry transaction")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Idempotency-Key; retries with the same key and body return the original result")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable UUID accountId,
            @Valid @RequestBody DepositRequest request) {

        UUID txId = idempotencyService.execute(idempotencyKey, DEPOSIT_SCOPE,
                accountId + "|" + request, () -> cashService.deposit(accountId, request));
        TransactionResponse body = TransactionResponse.from(ledgerService.getTransaction(txId));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + txId)).body(body);
    }

    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Remove cash from an account, recorded as a balanced double-entry transaction")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Idempotency-Key; retries with the same key and body return the original result")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable UUID accountId,
            @Valid @RequestBody WithdrawRequest request) {

        UUID txId = idempotencyService.execute(idempotencyKey, WITHDRAW_SCOPE,
                accountId + "|" + request, () -> cashService.withdraw(accountId, request));
        TransactionResponse body = TransactionResponse.from(ledgerService.getTransaction(txId));
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + txId)).body(body);
    }
}
