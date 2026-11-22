package com.rumi.billing.domain.service;

import com.rumi.billing.domain.model.CardDetails;
import com.rumi.billing.domain.model.Money;

import java.util.Objects;

/**
 * What the payment gateway is asked to charge. Its {@code toString} only shows the brand and last digits of the
 * card, through {@link CardDetails#toString()}.
 */
public record ChargeRequest(Money amount, CardDetails card) {

    public ChargeRequest {
        Objects.requireNonNull(amount, "Charge amount is required");
        Objects.requireNonNull(card, "Card is required");
    }
}
