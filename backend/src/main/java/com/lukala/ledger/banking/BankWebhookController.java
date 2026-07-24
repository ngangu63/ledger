package com.lukala.ledger.banking;

import com.lukala.ledger.banking.dto.SettlementEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives asynchronous settlement callbacks from the bank rail. Applying a
 * terminal state is idempotent, so retried callbacks are safe.
 *
 * <p>Production hardening: authenticate the caller by verifying a provider
 * signature (HMAC) and expose this path publicly, rather than gating it behind the
 * service JWT as the sandbox does.
 */
@RestController
@RequestMapping("/api/v1/banking/webhooks")
@Tag(name = "Banking", description = "Bank transfers (ACH / wire) and settlement")
public class BankWebhookController {

    private final BankingService bankingService;

    public BankWebhookController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping
    @Operation(summary = "Apply a bank settlement callback (SETTLED / RETURNED)")
    public ResponseEntity<Void> settle(@Valid @RequestBody SettlementEvent event) {
        bankingService.applySettlement(event.providerRef(), event.status());
        return ResponseEntity.accepted().build();
    }
}
