package com.lukala.ledger.payment;

import com.lukala.ledger.common.domain.BaseEntity;
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
 * A payment moves money between two ledger accounts through an external rail.
 * Its lifecycle status is tracked here; the authoritative money movement is the
 * linked ledger transaction ({@code transactionId}).
 */
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private PaymentDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 16)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentStatus status;

    @Embedded
    private Money money;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "failure_reason")
    private String failureReason;

    protected Payment() {
        // JPA
    }

    public Payment(UUID id, PaymentDirection direction, PaymentMethod method, Money money,
                   UUID sourceAccountId, UUID destinationAccountId, String externalRef) {
        this.id = id;
        this.direction = direction;
        this.method = method;
        this.money = money;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.externalRef = externalRef;
        this.status = PaymentStatus.PENDING;
    }

    public void markCaptured(String provider, String providerRef, UUID transactionId) {
        this.provider = provider;
        this.providerRef = providerRef;
        this.transactionId = transactionId;
        this.status = PaymentStatus.CAPTURED;
    }

    public void markFailed(String provider, String failureReason) {
        this.provider = provider;
        this.failureReason = failureReason;
        this.status = PaymentStatus.FAILED;
    }

    public void markReversed() {
        this.status = PaymentStatus.REVERSED;
    }

    public UUID getId() {
        return id;
    }

    public PaymentDirection getDirection() {
        return direction;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Money getMoney() {
        return money;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
