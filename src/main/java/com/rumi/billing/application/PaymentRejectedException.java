package com.rumi.billing.application;

import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.service.DeclineReason;

import java.util.Objects;
import java.util.Optional;

/**
 * Raised after the gateway declined a payment (402). The attempt has already been stored as {@code REJECTED}.
 * The decline reason is unknown when a stored rejection is answered again for a repeated Idempotency-Key.
 */
public class PaymentRejectedException extends RuntimeException {

    public static final String MESSAGE = "The payment was declined.";

    private final transient Payment payment;
    private final DeclineReason declineReason;

    public PaymentRejectedException(Payment payment, Optional<DeclineReason> declineReason) {
        super(MESSAGE);
        this.payment = Objects.requireNonNull(payment, "Payment is required");
        this.declineReason = declineReason.orElse(null);
    }

    public Payment payment() {
        return payment;
    }

    public Optional<DeclineReason> declineReason() {
        return Optional.ofNullable(declineReason);
    }
}
