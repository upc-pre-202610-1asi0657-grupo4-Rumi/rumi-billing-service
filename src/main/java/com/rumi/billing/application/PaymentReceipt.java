package com.rumi.billing.application;

import com.rumi.billing.domain.model.Payment;

/**
 * An approved payment, and whether it was made now ({@code true}) or found for a repeated Idempotency-Key.
 */
public record PaymentReceipt(Payment payment, boolean created) {
}
