package com.rumi.billing.application;

/**
 * Raised when a subscription cannot take a payment (409): it is cancelled, or it has expired and its building has
 * already chosen a new subscription.
 */
public class SubscriptionNotPayableException extends RuntimeException {

    public static final String CANCELLED = "The subscription is cancelled and cannot be paid.";
    public static final String REPLACED =
            "The subscription has expired and its building already has a new subscription; pay that one instead.";

    public SubscriptionNotPayableException(String message) {
        super(message);
    }
}
