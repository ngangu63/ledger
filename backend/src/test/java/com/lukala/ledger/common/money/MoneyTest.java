package com.lukala.ledger.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void addsAndSubtractsSameCurrency() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("2.50", "USD");
        assertThat(a.add(b)).isEqualTo(Money.of("12.50", "USD"));
        assertThat(a.subtract(b)).isEqualTo(Money.of("7.50", "USD"));
    }

    @Test
    void negateFlipsSign() {
        assertThat(Money.of("5.00", "USD").negate()).isEqualTo(Money.of("-5.00", "USD"));
    }

    @Test
    void rejectsArithmeticAcrossCurrencies() {
        assertThatThrownBy(() -> Money.of("1.00", "USD").add(Money.of("1.00", "EUR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void rejectsMorePrecisionThanCurrencyAllows() {
        // USD has 2 fraction digits; 3 decimals must be rejected, not silently rounded.
        assertThatThrownBy(() -> Money.of("1.005", "USD"))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void rejectsUnknownCurrency() {
        assertThatThrownBy(() -> Money.of("1.00", "ZZZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO-4217");
    }

    @Test
    void equalsComparesByValueNotScale() {
        assertThat(Money.of("1.00", "USD")).isEqualTo(Money.of(new BigDecimal("1.0000"), "USD"));
        assertThat(Money.of("1.00", "USD").hashCode())
                .isEqualTo(Money.of(new BigDecimal("1.0000"), "USD").hashCode());
    }

    @Test
    void signHelpers() {
        assertThat(Money.zero("USD").isZero()).isTrue();
        assertThat(Money.of("1.00", "USD").isPositive()).isTrue();
        assertThat(Money.of("-1.00", "USD").isNegative()).isTrue();
    }
}
