package com.rumi.billing.domain.model;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * How often a {@link Plan} is paid. Expiry dates use calendar arithmetic in UTC, so that 31 January plus one month
 * is the last day of February and not 3 March.
 */
public enum BillingCycle {

    MONTHLY,
    ANNUAL;

    public Instant nextExpiry(Instant from) {
        Objects.requireNonNull(from, "The start of the cycle is required");
        var start = from.atZone(ZoneOffset.UTC);
        var end = switch (this) {
            case MONTHLY -> start.plusMonths(1);
            case ANNUAL -> start.plusYears(1);
        };
        return end.toInstant();
    }
}
