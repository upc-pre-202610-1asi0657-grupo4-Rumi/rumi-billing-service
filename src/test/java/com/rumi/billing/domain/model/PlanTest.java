package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanTest {

    private static final UUID ID = UUID.fromString("0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001");
    private static final Money PRICE = new Money(new BigDecimal("149.00"), "PEN");

    @Test
    void createsAPlan() {
        Plan plan = new Plan(ID, "MONTHLY", "Plan mensual", BillingCycle.MONTHLY, PRICE, 20);

        assertThat(plan.getId()).isEqualTo(ID);
        assertThat(plan.getCode()).isEqualTo("MONTHLY");
        assertThat(plan.getName()).isEqualTo("Plan mensual");
        assertThat(plan.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(plan.getPrice()).isEqualTo(PRICE);
        assertThat(plan.getSensorLimit()).isEqualTo(20);
    }

    @Test
    void rejectsABlankCodeOrName() {
        assertThatThrownBy(() -> new Plan(ID, " ", "Plan mensual", BillingCycle.MONTHLY, PRICE, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Plan code is required");
        assertThatThrownBy(() -> new Plan(ID, "MONTHLY", "", BillingCycle.MONTHLY, PRICE, 20))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Plan name is required");
    }

    @Test
    void rejectsASensorLimitThatIsNotPositive() {
        assertThatThrownBy(() -> new Plan(ID, "MONTHLY", "Plan mensual", BillingCycle.MONTHLY, PRICE, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Sensor limit must be greater than zero");
    }

    @Test
    void requiresAnIdACycleAndAPrice() {
        assertThatThrownBy(() -> new Plan(null, "MONTHLY", "Plan mensual", BillingCycle.MONTHLY, PRICE, 20))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Plan(ID, "MONTHLY", "Plan mensual", null, PRICE, 20))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Plan(ID, "MONTHLY", "Plan mensual", BillingCycle.MONTHLY, null, 20))
                .isInstanceOf(NullPointerException.class);
    }
}
