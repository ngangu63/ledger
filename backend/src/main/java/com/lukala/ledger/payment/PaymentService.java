package com.lukala.ledger.payment;

import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.common.money.Money;
import com.lukala.ledger.ledger.LedgerService;
import com.lukala.ledger.ledger.PostingDirection;
import com.lukala.ledger.ledger.dto.CreateTransactionRequest;
import com.lukala.ledger.ledger.dto.PostingRequest;
import com.lukala.ledger.payment.dto.CreatePaymentRequest;
import com.lukala.ledger.payment.provider.ChargeCommand;
import com.lukala.ledger.payment.provider.ChargeResult;
import com.lukala.ledger.payment.provider.PaymentProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates a payment: charge the external rail, then record the resulting
 * money movement in the ledger as a balanced transaction. On decline, the
 * payment is marked FAILED and no ledger entry is written.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final LedgerService ledgerService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProvider paymentProvider,
                          LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public UUID charge(CreatePaymentRequest request) {
        Money amount = Money.of(request.amount(), request.currency());
        Payment payment = new Payment(UUID.randomUUID(), request.direction(), request.method(),
                amount, request.sourceAccountId(), request.destinationAccountId(),
                request.externalRef());
        paymentRepository.save(payment);

        ChargeResult result = paymentProvider.charge(new ChargeCommand(
                payment.getId(), request.direction(), request.method(), amount,
                request.instrumentToken(), request.externalRef()));

        if (!result.approved()) {
            payment.markFailed(paymentProvider.name(), result.failureReason());
            paymentRepository.save(payment);
            return payment.getId();
        }

        // Record the money movement: debit destination, credit source.
        UUID txId = ledgerService.post(new CreateTransactionRequest(
                request.currency(),
                "payment:" + payment.getId(),
                "Payment " + payment.getId() + " (" + paymentProvider.name() + ")",
                List.of(
                        new PostingRequest(request.destinationAccountId(), PostingDirection.DEBIT,
                                request.amount()),
                        new PostingRequest(request.sourceAccountId(), PostingDirection.CREDIT,
                                request.amount()))));

        payment.markCaptured(paymentProvider.name(), result.providerRef(), txId);
        paymentRepository.save(payment);
        return payment.getId();
    }

    /**
     * Refunds a captured payment: asks the rail to refund, then writes an
     * offsetting ledger entry via {@link LedgerService#reverse} so the books stay
     * balanced. Reuses the reversal path rather than duplicating posting logic.
     */
    @Transactional
    public UUID refund(UUID paymentId) {
        Payment payment = get(paymentId);
        if (payment.getStatus() != PaymentStatus.CAPTURED
                && payment.getStatus() != PaymentStatus.SETTLED) {
            throw new BusinessRuleException("PAYMENT_NOT_REFUNDABLE",
                    "Payment " + paymentId + " is " + payment.getStatus()
                            + " and cannot be refunded.");
        }
        ChargeResult result = paymentProvider.refund(payment.getProviderRef());
        if (!result.approved()) {
            throw new BusinessRuleException("REFUND_DECLINED",
                    "Refund declined by " + paymentProvider.name() + ": " + result.failureReason());
        }
        // Offsetting ledger entry keeps the double-entry books balanced.
        ledgerService.reverse(payment.getTransactionId(), "refund:" + payment.getId());
        payment.markReversed();
        paymentRepository.save(payment);
        return payment.getId();
    }

    @Transactional(readOnly = true)
    public Payment get(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", id));
    }
}
