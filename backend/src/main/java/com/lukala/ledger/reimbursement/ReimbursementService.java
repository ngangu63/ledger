package com.lukala.ledger.reimbursement;

import com.lukala.ledger.common.exception.ResourceNotFoundException;
import com.lukala.ledger.common.money.Money;
import com.lukala.ledger.payment.PaymentDirection;
import com.lukala.ledger.payment.PaymentMethod;
import com.lukala.ledger.payment.PaymentService;
import com.lukala.ledger.payment.dto.CreatePaymentRequest;
import com.lukala.ledger.reimbursement.dto.SubmitReimbursementRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reimbursement workflow: submit → approve/reject → pay. Paying an approved
 * reimbursement issues an OUTBOUND bank payment, which records the money movement
 * in the ledger.
 */
@Service
public class ReimbursementService {

    private final ReimbursementRepository repository;
    private final PaymentService paymentService;

    public ReimbursementService(ReimbursementRepository repository, PaymentService paymentService) {
        this.repository = repository;
        this.paymentService = paymentService;
    }

    @Transactional
    public Reimbursement submit(SubmitReimbursementRequest request) {
        Money amount = Money.of(request.amount(), request.currency());
        Reimbursement reimbursement = new Reimbursement(UUID.randomUUID(), request.requester(),
                amount, request.description(), request.fundingAccountId(), request.payeeAccountId());
        return repository.save(reimbursement);
    }

    @Transactional
    public Reimbursement approve(UUID id, String approver) {
        Reimbursement reimbursement = get(id);
        reimbursement.approve(approver);
        return repository.save(reimbursement);
    }

    @Transactional
    public Reimbursement reject(UUID id, String approver) {
        Reimbursement reimbursement = get(id);
        reimbursement.reject(approver);
        return repository.save(reimbursement);
    }

    @Transactional
    public Reimbursement pay(UUID id) {
        Reimbursement reimbursement = get(id);
        // Guard the transition before touching the payment rail, so a non-approved
        // reimbursement never triggers an external charge.
        if (reimbursement.getStatus() != ReimbursementStatus.APPROVED) {
            throw new com.lukala.ledger.common.exception.BusinessRuleException(
                    "ILLEGAL_STATE_TRANSITION",
                    "Cannot pay a reimbursement in status " + reimbursement.getStatus() + ".");
        }
        UUID paymentId = paymentService.charge(new CreatePaymentRequest(
                PaymentDirection.OUTBOUND,
                PaymentMethod.BANK,
                reimbursement.getMoney().getAmount(),
                reimbursement.getMoney().getCurrency(),
                reimbursement.getFundingAccountId(),
                reimbursement.getPayeeAccountId(),
                null,
                "reimbursement:" + reimbursement.getId()));
        reimbursement.markPaid(paymentId);
        return repository.save(reimbursement);
    }

    @Transactional(readOnly = true)
    public Reimbursement get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Reimbursement", id));
    }
}
