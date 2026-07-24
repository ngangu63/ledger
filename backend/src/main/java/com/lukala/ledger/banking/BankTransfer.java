package com.lukala.ledger.banking;

import com.lukala.ledger.common.domain.BaseEntity;
import com.lukala.ledger.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A bank transfer moves money between two ledger accounts over an external rail
 * (ACH/wire). It is recorded in the ledger at initiation and reaches a terminal
 * state asynchronously: {@code SETTLED} on confirmation, or {@code RETURNED} (with
 * an offsetting ledger entry) on a return. The authoritative money movement is the
 * linked {@code transactionId}.
 */
@Entity
@Table(name = "bank_transfer")
public class BankTransfer extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rail", nullable = false, length = 16)
    private BankRail rail;

    @Embedded
    private Money money;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BankTransferStatus status;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_ref")
    private String providerRef;

    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "settled_at")
    private Instant settledAt;

    protected BankTransfer() {
        // JPA
    }

    public BankTransfer(UUID id, BankRail rail, Money money, UUID sourceAccountId,
                        UUID destinationAccountId, String externalRef) {
        this.id = id;
        this.rail = rail;
        this.money = money;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.externalRef = externalRef;
        this.status = BankTransferStatus.PENDING;
    }

    public void markInitiated(String provider, String providerRef, UUID transactionId) {
        this.provider = provider;
        this.providerRef = providerRef;
        this.transactionId = transactionId;
    }

    public void markSettled(Instant settledAt) {
        this.status = BankTransferStatus.SETTLED;
        this.settledAt = settledAt;
    }

    public void markReturned() {
        this.status = BankTransferStatus.RETURNED;
    }

    public boolean isPending() {
        return status == BankTransferStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public BankRail getRail() {
        return rail;
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

    public BankTransferStatus getStatus() {
        return status;
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

    public Instant getSettledAt() {
        return settledAt;
    }
}
