package com.lukala.ledger.ledger;

import com.lukala.ledger.common.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A journal entry: a group of postings that together must balance (debits ==
 * credits per currency). Append-only — once posted, a transaction is never
 * modified. To undo, post a reversing transaction referencing this one.
 */
@Entity
@Table(name = "ledger_transaction")
public class LedgerTransaction extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Optional caller-supplied reference (e.g. payment id, invoice number). */
    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransactionStatus status;

    /** Set on a reversing entry: the id of the transaction being reversed. */
    @Column(name = "reversal_of_id", updatable = false)
    private UUID reversalOfId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Posting> postings = new ArrayList<>();

    protected LedgerTransaction() {
        // JPA
    }

    public LedgerTransaction(UUID id, String externalRef, String description) {
        this.id = id;
        this.externalRef = externalRef;
        this.description = description;
        this.status = TransactionStatus.POSTED;
    }

    public void addPosting(Posting posting) {
        postings.add(posting);
    }

    public void markReversed() {
        this.status = TransactionStatus.REVERSED;
    }

    public void setReversalOfId(UUID reversalOfId) {
        this.reversalOfId = reversalOfId;
    }

    public UUID getId() {
        return id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public List<Posting> getPostings() {
        return Collections.unmodifiableList(postings);
    }
}
