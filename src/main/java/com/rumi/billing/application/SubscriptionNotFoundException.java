package com.rumi.billing.application;

/**
 * Raised when a subscription does not exist or belongs to another administrator; both get the same answer (404).
 */
public class SubscriptionNotFoundException extends RuntimeException {

    public static final String MESSAGE = "The subscription does not exist.";

    public SubscriptionNotFoundException() {
        super(MESSAGE);
    }
}
