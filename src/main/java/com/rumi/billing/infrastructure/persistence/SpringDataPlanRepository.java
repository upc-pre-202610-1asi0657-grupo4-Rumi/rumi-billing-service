package com.rumi.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPlanRepository extends JpaRepository<PlanJpaEntity, UUID> {

    List<PlanJpaEntity> findAllByOrderByPriceAsc();

    Optional<PlanJpaEntity> findByCode(String code);
}
