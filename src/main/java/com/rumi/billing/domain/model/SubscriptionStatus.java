package com.rumi.billing.domain.model;

/**
 * Lifecycle of a {@link Subscription}. A building has at most one subscription that is open, that is,
 * {@code PENDING_PAYMENT} or {@code ACTIVE}.
 */
public enum SubscriptionStatus {

    PENDING_PAYMENT,
    ACTIVE,
    EXPIRED,
    CANCELLED;

    public boolean isOpen() {
        return this == PENDING_PAYMENT || this == ACTIVE;
    }
}
