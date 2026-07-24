package com.lukala.ledger.ledger;

import com.lukala.ledger.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A single leg of a transaction: a debit or credit of a specific amount against
 * one account. Postings are immutable once written — corrections are made via a
 * reversing transaction, never by editing a posting.
 */
@Entity
@Table(name = "posting")
public class Posting {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private LedgerTransaction transaction;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, updatable = false, length = 8)
    private PostingDirection direction;

    @Embedded
    private Money money;

    protected Posting() {
        // JPA
    }

    public Posting(UUID id, LedgerTransaction transaction, UUID accountId,
                   PostingDirection direction, Money money) {
        this.id = id;
        this.transaction = transaction;
        this.accountId = accountId;
        this.direction = direction;
        this.money = money;
    }

    /**
     * Signed contribution to an account's balance under the debit-positive
     * convention: a debit adds, a credit subtracts.
     */
    public Money signedAmount() {
        return direction == PostingDirection.DEBIT ? money : money.negate();
    }

    public UUID getId() {
        return id;
    }

    public LedgerTransaction getTransaction() {
        return transaction;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public PostingDirection getDirection() {
        return direction;
    }

    public Money getMoney() {
        return money;
    }
}
