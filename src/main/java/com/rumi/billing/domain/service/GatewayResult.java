package com.rumi.billing.domain.service;

import java.util.Objects;
import java.util.Optional;

/**
 * Answer of the payment gateway to a charge: approved or declined, with the reference the gateway gave to it.
 */
public record GatewayResult(boolean approved, String reference, Optional<DeclineReason> declineReason) {

    public GatewayResult {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Gateway reference is required");
        }
        Objects.requireNonNull(declineReason, "Decline reason is required");
        if (approved == declineReason.isPresent()) {
            throw new IllegalArgumentException("Only a declined charge has a decline reason");
        }
    }

    public static GatewayResult approved(String reference) {
        return new GatewayResult(true, reference, Optional.empty());
    }

    public static GatewayResult declined(String reference, DeclineReason reason) {
        return new GatewayResult(false, reference, Optional.of(Objects.requireNonNull(reason, "Decline reason is required")));
    }
}
