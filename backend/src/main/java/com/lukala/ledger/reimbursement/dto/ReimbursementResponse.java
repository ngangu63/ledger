package com.lukala.ledger.reimbursement.dto;

import com.lukala.ledger.reimbursement.Reimbursement;
import com.lukala.ledger.reimbursement.ReimbursementStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReimbursementResponse(
        UUID id,
        String requester,
        BigDecimal amount,
        String currency,
        String description,
        ReimbursementStatus status,
        UUID fundingAccountId,
        UUID payeeAccountId,
        String decidedBy,
        UUID paymentId,
        Instant createdAt) {

    public static ReimbursementResponse from(Reimbursement r) {
        return new ReimbursementResponse(
                r.getId(), r.getRequester(), r.getMoney().getAmount(), r.getMoney().getCurrency(),
                r.getDescription(), r.getStatus(), r.getFundingAccountId(), r.getPayeeAccountId(),
                r.getDecidedBy(), r.getPaymentId(), r.getCreatedAt());
    }
}
