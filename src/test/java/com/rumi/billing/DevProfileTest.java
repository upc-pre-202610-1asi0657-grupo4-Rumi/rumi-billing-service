package com.rumi.billing;

import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.repository.PlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The dev profile runs on in-memory H2 with no external infrastructure, and seeds the same two plans as
 * docs/schema.sql.
 */
@SpringBootTest
@ActiveProfiles("dev")
class DevProfileTest {

    @Autowired
    private PlanRepository plans;

    @Test
    void seedsTheTwoPlansInH2() {
        assertThat(plans.findAll()).extracting(Plan::getCode).containsExactly("MONTHLY", "ANNUAL");
    }

    @Test
    void seedsTheSamePricesAndSensorLimitAsDocsSchema() {
        assertThat(plans.findAll())
                .extracting(plan -> plan.getCode() + " " + plan.getPrice().amount().toPlainString() + " "
                        + plan.getPrice().currency() + " " + plan.getSensorLimit())
                .containsExactly("MONTHLY 199.00 PEN 10", "ANNUAL 1990.00 PEN 10");
    }
}
