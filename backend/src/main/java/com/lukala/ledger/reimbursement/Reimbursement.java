package com.lukala.ledger.reimbursement;

import com.lukala.ledger.common.domain.BaseEntity;
import com.lukala.ledger.common.exception.BusinessRuleException;
import com.lukala.ledger.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A reimbursement request that flows through submit → approve → pay. State
 * transitions are guarded so an amount can only be paid once, and only after
 * approval.
 */
@Entity
@Table(name = "reimbursement")
public class Reimbursement extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "requester", nullable = false)
    private String requester;

    @Embedded
    private Money money;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReimbursementStatus status;

    /** Account funding the payout (source). */
    @Column(name = "funding_account_id", nullable = false)
    private UUID fundingAccountId;

    /** Account the payee receives into (destination). */
    @Column(name = "payee_account_id", nullable = false)
    private UUID payeeAccountId;

    @Column(name = "decided_by")
    private String decidedBy;

    @Column(name = "payment_id")
    private UUID paymentId;

    protected Reimbursement() {
        // JPA
    }

    public Reimbursement(UUID id, String requester, Money money, String description,
                         UUID fundingAccountId, UUID payeeAccountId) {
        this.id = id;
        this.requester = requester;
        this.money = money;
        this.description = description;
        this.fundingAccountId = fundingAccountId;
        this.payeeAccountId = payeeAccountId;
        this.status = ReimbursementStatus.SUBMITTED;
    }

    public void approve(String approver) {
        requireStatus(ReimbursementStatus.SUBMITTED, "approve");
        this.status = ReimbursementStatus.APPROVED;
        this.decidedBy = approver;
    }

    public void reject(String approver) {
        requireStatus(ReimbursementStatus.SUBMITTED, "reject");
        this.status = ReimbursementStatus.REJECTED;
        this.decidedBy = approver;
    }

    public void markPaid(UUID paymentId) {
        requireStatus(ReimbursementStatus.APPROVED, "pay");
        this.status = ReimbursementStatus.PAID;
        this.paymentId = paymentId;
    }

    private void requireStatus(ReimbursementStatus expected, String action) {
        if (this.status != expected) {
            throw new BusinessRuleException("ILLEGAL_STATE_TRANSITION",
                    "Cannot " + action + " a reimbursement in status " + status + ".");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getRequester() {
        return requester;
    }

    public Money getMoney() {
        return money;
    }

    public String getDescription() {
        return description;
    }

    public ReimbursementStatus getStatus() {
        return status;
    }

    public UUID getFundingAccountId() {
        return fundingAccountId;
    }

    public UUID getPayeeAccountId() {
        return payeeAccountId;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public UUID getPaymentId() {
        return paymentId;
    }
}
