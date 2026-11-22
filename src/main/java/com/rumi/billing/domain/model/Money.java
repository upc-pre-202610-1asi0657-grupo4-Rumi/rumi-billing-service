package com.rumi.billing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A positive amount of money with two decimals in an ISO 4217 currency.
 */
public record Money(BigDecimal amount, String currency) {

    public static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("Amount must have at most " + SCALE + " decimals");
        }
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO 4217 code");
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
