package com.rumi.billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    List<PaymentJpaEntity> findBySubscriptionIdOrderByCreatedAtAscIdAsc(UUID subscriptionId);

    @Query("select p.id from PaymentJpaEntity p where p.subscriptionId = :subscriptionId")
    Set<UUID> findIdsBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
