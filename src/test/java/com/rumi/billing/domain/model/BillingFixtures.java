package com.rumi.billing.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Plans, ids and instants shared by the domain tests.
 */
public final class BillingFixtures {

    public static final UUID OWNER_ID = UUID.fromString("7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f");
    public static final UUID BUILDING_ID = UUID.fromString("3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b");
    public static final UUID SUBSCRIPTION_ID = UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");
    public static final Instant NOW = Instant.parse("2026-01-31T15:00:00Z");

    public static final Plan MONTHLY = new Plan(UUID.fromString("0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001"),
            "MONTHLY", "Plan mensual", BillingCycle.MONTHLY, new Money(new BigDecimal("149.00"), "PEN"), 20);
    public static final Plan ANNUAL = new Plan(UUID.fromString("0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002"),
            "ANNUAL", "Plan anual", BillingCycle.ANNUAL, new Money(new BigDecimal("1490.00"), "PEN"), 20);

    private BillingFixtures() {
    }

    public static Payment approvedPayment(Subscription subscription, Instant at) {
        return Payment.approved(UUID.randomUUID(), subscription.getId(), subscription.getPlan().getPrice(),
                CardBrand.VISA, "1111", "sim_" + UUID.randomUUID(), null, at);
    }

    public static Payment rejectedPayment(Subscription subscription, Instant at) {
        return Payment.rejected(UUID.randomUUID(), subscription.getId(), subscription.getPlan().getPrice(),
                CardBrand.VISA, "0002", "sim_" + UUID.randomUUID(), null, at);
    }
}
