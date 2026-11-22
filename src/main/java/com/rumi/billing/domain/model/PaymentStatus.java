package com.rumi.billing.domain.model;

/**
 * Outcome of a payment attempt. Rejected attempts are kept too, so that the history shows them (US07).
 */
public enum PaymentStatus {

    APPROVED,
    REJECTED
}
