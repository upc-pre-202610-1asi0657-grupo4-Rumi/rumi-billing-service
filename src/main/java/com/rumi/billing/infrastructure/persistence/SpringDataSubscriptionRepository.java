package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubscriptionJpaEntity s where s.id = :id")
    Optional<SubscriptionJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SubscriptionJpaEntity s where s.buildingId = :buildingId and s.status in :statuses")
    Optional<SubscriptionJpaEntity> findByBuildingIdAndStatusInForUpdate(
            @Param("buildingId") UUID buildingId, @Param("statuses") Collection<SubscriptionStatus> statuses);
}
