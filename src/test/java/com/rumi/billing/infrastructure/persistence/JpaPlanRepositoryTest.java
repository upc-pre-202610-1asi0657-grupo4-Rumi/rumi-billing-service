package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.PostgresTestContainer;
import com.rumi.billing.domain.model.BillingCycle;
import com.rumi.billing.domain.model.Plan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestContainer.class, JpaPlanRepository.class})
class JpaPlanRepositoryTest {

    @Autowired
    private JpaPlanRepository repository;

    @Test
    void readsTheTwoSeededPlansCheapestFirst() {
        assertThat(repository.findAll()).extracting(Plan::getCode).containsExactly("MONTHLY", "ANNUAL");
    }

    @Test
    void theSeededPlansCost199And1990PenWithTaxIncludedAndAllowTenSensors() {
        assertThat(repository.findByCode("MONTHLY")).hasValueSatisfying(plan -> {
            assertThat(plan.getPrice().amount()).isEqualByComparingTo("199.00");
            assertThat(plan.getPrice().currency()).isEqualTo("PEN");
            assertThat(plan.getSensorLimit()).isEqualTo(10);
        });
        assertThat(repository.findByCode("ANNUAL")).hasValueSatisfying(plan -> {
            assertThat(plan.getPrice().amount()).isEqualByComparingTo("1990.00");
            assertThat(plan.getPrice().currency()).isEqualTo("PEN");
            assertThat(plan.getSensorLimit()).isEqualTo(10);
        });
    }

    @Test
    void findsAPlanByItsCodeWithItsExactPrice() {
        assertThat(repository.findByCode("ANNUAL")).hasValueSatisfying(plan -> {
            assertThat(plan.getId()).isEqualTo(UUID.fromString("0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002"));
            assertThat(plan.getName()).isEqualTo("Plan anual");
            assertThat(plan.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(plan.getPrice().amount()).isEqualByComparingTo("1990.00");
            assertThat(plan.getPrice().amount().scale()).isEqualTo(2);
            assertThat(plan.getPrice().currency()).isEqualTo("PEN");
            assertThat(plan.getSensorLimit()).isEqualTo(10);
        });
        assertThat(repository.findByCode("WEEKLY")).isEmpty();
    }
}
