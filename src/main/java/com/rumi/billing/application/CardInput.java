package com.rumi.billing.application;

import com.rumi.billing.domain.model.CardDetails;

import java.time.Instant;

/**
 * Card data as the client sent it, before validation. It never prints its values, so that it cannot leak into logs.
 */
public record CardInput(String number, String holderName, int expiryMonth, int expiryYear, String cvv) {

    CardDetails toCardDetails(Instant now) {
        return CardDetails.of(number, holderName, expiryMonth, expiryYear, cvv, now);
    }

    @Override
    public String toString() {
        return "CardInput[hidden]";
    }
}
