package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.Money;
import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.repository.PlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaPlanRepository implements PlanRepository {

    private final SpringDataPlanRepository springDataRepository;

    public JpaPlanRepository(SpringDataPlanRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<Plan> findAll() {
        return springDataRepository.findAllByOrderByPriceAsc().stream().map(JpaPlanRepository::toDomain).toList();
    }

    @Override
    public Optional<Plan> findByCode(String code) {
        return springDataRepository.findByCode(code).map(JpaPlanRepository::toDomain);
    }

    static Plan toDomain(PlanJpaEntity entity) {
        return new Plan(entity.getId(), entity.getCode(), entity.getName(), entity.getBillingCycle(),
                new Money(entity.getPrice(), entity.getCurrency()), entity.getSensorLimit());
    }
}
