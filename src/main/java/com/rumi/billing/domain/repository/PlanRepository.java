package com.rumi.billing.domain.repository;

import com.rumi.billing.domain.model.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanRepository {

    /**
     * Every plan, cheapest first.
     */
    List<Plan> findAll();

    Optional<Plan> findByCode(String code);
}
