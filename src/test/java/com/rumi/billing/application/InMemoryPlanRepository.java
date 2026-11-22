package com.rumi.billing.application;

import com.rumi.billing.domain.model.BillingFixtures;
import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.repository.PlanRepository;

import java.util.List;
import java.util.Optional;

public class InMemoryPlanRepository implements PlanRepository {

    private final List<Plan> plans = List.of(BillingFixtures.MONTHLY, BillingFixtures.ANNUAL);

    @Override
    public List<Plan> findAll() {
        return plans;
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return plans.stream().filter(plan -> plan.getCode().equals(code)).findFirst();
    }
}
