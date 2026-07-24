package com.lukala.ledger.banking;

import com.lukala.ledger.banking.dto.BankTransferResponse;
import com.lukala.ledger.banking.dto.InitiateTransferRequest;
import com.lukala.ledger.common.idempotency.IdempotencyService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bank-transfers")
@Tag(name = "Banking", description = "Bank transfers (ACH / wire) and settlement")
public class BankTransferController {

    private static final String SCOPE = "banking.transfer.initiate";

    private final BankingService bankingService;
    private final IdempotencyService idempotencyService;

    public BankTransferController(BankingService bankingService,
                                  IdempotencyService idempotencyService) {
        this.bankingService = bankingService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "Initiate a bank transfer and record it in the ledger")
    public ResponseEntity<BankTransferResponse> initiate(
            @Parameter(description = "Idempotency-Key; retries with the same key and body return the original result")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiateTransferRequest request) {
        UUID id = idempotencyService.execute(idempotencyKey, SCOPE, request.toString(),
                () -> bankingService.initiate(request));
        return ResponseEntity.created(URI.create("/api/v1/bank-transfers/" + id))
                .body(BankTransferResponse.from(bankingService.get(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a bank transfer by id")
    public BankTransferResponse get(@PathVariable UUID id) {
        return BankTransferResponse.from(bankingService.get(id));
    }
}
