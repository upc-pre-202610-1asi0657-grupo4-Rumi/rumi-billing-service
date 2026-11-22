package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Row of a subscription. Its payments are rows of {@link PaymentJpaEntity}, loaded and inserted by
 * {@link JpaSubscriptionRepository}. The owner and the building live in other services, so they have no foreign key.
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanJpaEntity plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SubscriptionJpaEntity() {
    }

    public SubscriptionJpaEntity(UUID id, UUID ownerId, UUID buildingId, PlanJpaEntity plan,
                                 SubscriptionStatus status, Instant startsAt, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.buildingId = buildingId;
        this.plan = plan;
        this.status = status;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public PlanJpaEntity getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
