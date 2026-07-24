package com.lukala.ledger.payment;

import com.lukala.ledger.common.idempotency.IdempotencyService;
import com.lukala.ledger.payment.dto.CreatePaymentRequest;
import com.lukala.ledger.payment.dto.PaymentResponse;
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
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Card and bank payments")
public class PaymentController {

    private static final String SCOPE = "payment.charge";
    private static final String REFUND_SCOPE = "payment.refund";

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @Operation(summary = "Charge a payment and record it in the ledger on capture")
    public ResponseEntity<PaymentResponse> charge(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        UUID id = idempotencyService.execute(idempotencyKey, SCOPE, request.toString(),
                () -> paymentService.charge(request));
        return ResponseEntity.created(URI.create("/api/v1/payments/" + id))
                .body(PaymentResponse.from(paymentService.get(id)));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund a captured payment, posting an offsetting ledger entry")
    public ResponseEntity<PaymentResponse> refund(
            @Parameter(description = "Idempotency-Key; retries with the same key return the original result")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable UUID id) {
        idempotencyService.execute(idempotencyKey, REFUND_SCOPE, "refund:" + id,
                () -> paymentService.refund(id));
        return ResponseEntity.ok(PaymentResponse.from(paymentService.get(id)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment by id")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentResponse.from(paymentService.get(id));
    }
}
