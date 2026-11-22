package com.rumi.billing.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The subscription of a building to a {@link Plan}, and the aggregate root of its {@link Payment}s.
 * <p>
 * Choosing a plan creates it as {@code PENDING_PAYMENT} (US05). An approved payment starts it, renews it from the
 * current expiry while it is active, or restarts it from now once it has expired (US24). A rejected payment is only
 * recorded. There is no scheduler yet: an {@code ACTIVE} subscription whose expiry has passed is reported as
 * {@code EXPIRED} by {@link #statusAt(Instant)} and stored as such by {@link #expireIfDue(Instant)}.
 */
public final class Subscription {

    private final UUID id;
    private final UUID ownerId;
    private final UUID buildingId;
    private final Plan plan;
    private final Instant createdAt;
    private final List<Payment> payments;
    private SubscriptionStatus status;
    private Instant startsAt;
    private Instant expiresAt;

    public Subscription(UUID id, UUID ownerId, UUID buildingId, Plan plan, SubscriptionStatus status, Instant startsAt,
                        Instant expiresAt, Instant createdAt, List<Payment> payments) {
        this.id = Objects.requireNonNull(id, "Subscription id is required");
        this.ownerId = Objects.requireNonNull(ownerId, "Subscription owner id is required");
        this.buildingId = Objects.requireNonNull(buildingId, "Subscription building id is required");
        this.plan = Objects.requireNonNull(plan, "Subscription plan is required");
        this.status = Objects.requireNonNull(status, "Subscription status is required");
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Subscription creation time is required");
        this.payments = new ArrayList<>(Objects.requireNonNull(payments, "Payments are required"));
        if (status == SubscriptionStatus.ACTIVE && (startsAt == null || expiresAt == null)) {
            throw new IllegalArgumentException("An active subscription needs its start and expiry");
        }
    }

    public static Subscription choose(UUID id, UUID ownerId, UUID buildingId, Plan plan, Instant now) {
        return new Subscription(id, ownerId, buildingId, plan, SubscriptionStatus.PENDING_PAYMENT, null, null, now,
                List.of());
    }

    /**
     * The status as of {@code now}: an active subscription whose expiry has been reached is expired.
     */
    public SubscriptionStatus statusAt(Instant now) {
        if (status == SubscriptionStatus.ACTIVE && !now.isBefore(expiresAt)) {
            return SubscriptionStatus.EXPIRED;
        }
        return status;
    }

    /**
     * Stores the {@code EXPIRED} status when the expiry has been reached, so that the building is free to choose a
     * new plan. Returns whether the status changed.
     */
    public boolean expireIfDue(Instant now) {
        if (status == SubscriptionStatus.ACTIVE && statusAt(now) == SubscriptionStatus.EXPIRED) {
            status = SubscriptionStatus.EXPIRED;
            return true;
        }
        return false;
    }

    /**
     * Whether the stored status blocks another subscription for the same building.
     */
    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isPayableAt(Instant now) {
        return statusAt(now) != SubscriptionStatus.CANCELLED;
    }

    public boolean belongsTo(UUID ownerId) {
        return this.ownerId.equals(ownerId);
    }

    public void registerApprovedPayment(Payment payment, Instant now) {
        requireOwnPayment(payment);
        if (!payment.isApproved()) {
            throw new IllegalArgumentException("The payment is not approved");
        }
        if (!payment.getAmount().equals(plan.getPrice())) {
            throw new IllegalArgumentException("The payment amount must be the price of the plan");
        }
        requirePayable(now);
        BillingCycle cycle = plan.getBillingCycle();
        if (statusAt(now) == SubscriptionStatus.ACTIVE) {
            expiresAt = cycle.nextExpiry(expiresAt);
        } else {
            startsAt = now;
            expiresAt = cycle.nextExpiry(now);
        }
        status = SubscriptionStatus.ACTIVE;
        payments.add(payment);
    }

    public void registerRejectedPayment(Payment payment) {
        requireOwnPayment(payment);
        if (payment.isApproved()) {
            throw new IllegalArgumentException("The payment is not rejected");
        }
        requirePayable(payment.getCreatedAt());
        payments.add(payment);
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

    public Plan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Optional<Instant> getStartsAt() {
        return Optional.ofNullable(startsAt);
    }

    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    @Override
    public String toString() {
        return "Subscription[id=" + id + ", buildingId=" + buildingId + ", plan=" + plan.getCode()
                + ", status=" + status + "]";
    }

    private void requireOwnPayment(Payment payment) {
        Objects.requireNonNull(payment, "Payment is required");
        if (!payment.getSubscriptionId().equals(id)) {
            throw new IllegalArgumentException("The payment belongs to another subscription");
        }
    }

    private void requirePayable(Instant now) {
        if (!isPayableAt(now)) {
            throw new IllegalStateException("A cancelled subscription cannot be paid");
        }
    }
}
