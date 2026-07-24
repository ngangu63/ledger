package com.lukala.ledger.common.money;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable money value object: a {@link BigDecimal} amount paired with an
 * ISO-4217 currency. Never use {@code double}/{@code float} for money.
 *
 * <p>Amounts are normalized to the currency's default fraction digits (e.g. 2 for
 * USD/EUR) using {@link java.math.RoundingMode#UNNECESSARY} — an amount with more
 * precision than the currency allows is rejected rather than silently rounded.
 * Arithmetic across different currencies is rejected.
 */
@Embeddable
public final class Money {

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private final BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private final String currency;

    protected Money() {
        // Required by JPA.
        this.amount = null;
        this.currency = null;
    }

    private Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currencyCode, "currency must not be null");
        Currency currency = parseCurrency(currencyCode);
        BigDecimal normalized = amount.setScale(currency.getDefaultFractionDigits(),
                java.math.RoundingMode.UNNECESSARY);
        return new Money(normalized, currency.getCurrencyCode());
    }

    public static Money of(String amount, String currencyCode) {
        return of(new BigDecimal(amount), currencyCode);
    }

    public static Money zero(String currencyCode) {
        return of(BigDecimal.ZERO, currencyCode);
    }

    private static Currency parseCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown ISO-4217 currency: " + code);
        }
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
