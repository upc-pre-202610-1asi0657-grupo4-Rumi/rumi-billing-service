package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void keepsTheAmountWithTwoDecimals() {
        Money money = new Money(new BigDecimal("149"), "PEN");

        assertThat(money.amount()).isEqualByComparingTo("149.00");
        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.currency()).isEqualTo("PEN");
    }

    @Test
    void twoAmountsWithTheSameValueAreEqual() {
        assertThat(new Money(new BigDecimal("1490.0"), "PEN")).isEqualTo(new Money(new BigDecimal("1490.00"), "PEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1.00"})
    void rejectsAnAmountThatIsNotPositive(String amount) {
        assertThatThrownBy(() -> new Money(new BigDecimal(amount), "PEN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be greater than zero");
    }

    @Test
    void rejectsMoreThanTwoDecimals() {
        assertThatThrownBy(() -> new Money(new BigDecimal("149.001"), "PEN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must have at most 2 decimals");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "pen", "PE", "PENS", "P3N"})
    void rejectsACurrencyThatIsNotAnIsoCode(String currency) {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, currency))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency must be a three-letter ISO 4217 code");
    }

    @Test
    void requiresAnAmountAndACurrency() {
        assertThatThrownBy(() -> new Money(null, "PEN")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null)).isInstanceOf(NullPointerException.class);
    }
}
