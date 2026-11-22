package com.rumi.billing.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A subscription plan that an administrator can choose for a building (US05). Plans are reference data.
 */
public final class Plan {

    private final UUID id;
    private final String code;
    private final String name;
    private final BillingCycle billingCycle;
    private final Money price;
    private final int sensorLimit;

    public Plan(UUID id, String code, String name, BillingCycle billingCycle, Money price, int sensorLimit) {
        this.id = Objects.requireNonNull(id, "Plan id is required");
        this.code = requireText(code, "Plan code is required");
        this.name = requireText(name, "Plan name is required");
        this.billingCycle = Objects.requireNonNull(billingCycle, "Billing cycle is required");
        this.price = Objects.requireNonNull(price, "Plan price is required");
        if (sensorLimit <= 0) {
            throw new IllegalArgumentException("Sensor limit must be greater than zero");
        }
        this.sensorLimit = sensorLimit;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public Money getPrice() {
        return price;
    }

    public int getSensorLimit() {
        return sensorLimit;
    }

    @Override
    public String toString() {
        return "Plan[code=" + code + ", billingCycle=" + billingCycle + ", price=" + price + "]";
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
