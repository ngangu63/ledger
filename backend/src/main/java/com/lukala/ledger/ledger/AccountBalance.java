package com.lukala.ledger.ledger;

import com.lukala.ledger.common.domain.BaseEntity;
import com.lukala.ledger.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A checkpointed balance for an account, updated within the same DB transaction
 * that writes the postings. Stored under the debit-positive convention
 * ({@code debits - credits}); callers translate to a natural balance using the
 * account type. The optimistic-lock {@code version} from {@link BaseEntity}
 * serializes concurrent postings to the same account, preventing lost updates.
 */
@Entity
@Table(name = "account_balance")
public class AccountBalance extends BaseEntity {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "signed_amount", nullable = false, precision = 19, scale = 4)
    private java.math.BigDecimal signedAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    protected AccountBalance() {
        // JPA
    }

    public AccountBalance(UUID accountId, String currency) {
        this.accountId = accountId;
        this.currency = currency;
        this.signedAmount = java.math.BigDecimal.ZERO;
    }

    /** Apply a signed posting amount (debit positive, credit negative). */
    public void apply(Money signed) {
        if (!signed.getCurrency().equals(currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch applying posting to balance: "
                            + signed.getCurrency() + " vs " + currency);
        }
        this.signedAmount = this.signedAmount.add(signed.getAmount());
    }

    public Money signedBalance() {
        return Money.of(signedAmount, currency);
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCurrency() {
        return currency;
    }
}
