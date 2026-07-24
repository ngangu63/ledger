package com.lukala.ledger.account;

import com.lukala.ledger.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A ledger account. Accounts are the anchors postings attach to; an account has a
 * fixed currency and type. Accounts themselves do not store a balance — the
 * balance is derived from postings and checkpointed in the balance table.
 */
@Entity
@Table(name = "account")
public class Account extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private AccountType type;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status;

    protected Account() {
        // JPA
    }

    public Account(UUID id, String name, AccountType type, String currency) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
    }

    public boolean isPostable() {
        return status == AccountStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
