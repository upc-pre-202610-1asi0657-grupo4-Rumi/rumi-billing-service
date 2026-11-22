package com.rumi.billing.application;

/**
 * Raised when an Idempotency-Key already used for one subscription is sent to pay another one (409).
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public static final String MESSAGE = "The Idempotency-Key was already used for another subscription.";

    public IdempotencyKeyConflictException() {
        super(MESSAGE);
    }
}
