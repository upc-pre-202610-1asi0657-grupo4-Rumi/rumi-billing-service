package com.rumi.billing.application;

import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.repository.SubscriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps copies of the aggregates, so that a change is only seen after {@link #save}, and checks the same unique
 * rules as the PostgreSQL indexes: one open subscription per building and unique idempotency keys.
 */
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<UUID, Subscription> subscriptions = new LinkedHashMap<>();
    private Runnable beforeNextSave = () -> { };

    /**
     * Runs once just before the next save, to play a concurrent request that commits first.
     */
    public void beforeNextSave(Runnable action) {
        this.beforeNextSave = action;
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return Optional.ofNullable(subscriptions.get(id)).map(InMemorySubscriptionRepository::copy);
    }

    @Override
    public Optional<Subscription> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    @Override
    public Optional<Subscription> findOpenByBuildingIdForUpdate(UUID buildingId) {
        return subscriptions.values().stream()
                .filter(subscription -> subscription.getBuildingId().equals(buildingId) && subscription.isOpen())
                .findFirst()
                .map(InMemorySubscriptionRepository::copy);
    }

    @Override
    public Optional<Payment> findPaymentByIdempotencyKey(String idempotencyKey) {
        return subscriptions.values().stream()
                .flatMap(subscription -> subscription.getPayments().stream())
                .filter(payment -> payment.getIdempotencyKey().filter(idempotencyKey::equals).isPresent())
                .findFirst();
    }

    @Override
    public Subscription save(Subscription subscription) {
        Runnable action = beforeNextSave;
        beforeNextSave = () -> { };
        action.run();
        boolean anotherOpen = subscription.isOpen() && subscriptions.values().stream()
                .anyMatch(other -> !other.getId().equals(subscription.getId())
                        && other.getBuildingId().equals(subscription.getBuildingId()) && other.isOpen());
        if (anotherOpen) {
            throw new DataIntegrityViolationException("uq_subscriptions_open_building");
        }
        for (Payment payment : subscription.getPayments()) {
            boolean keyTaken = payment.getIdempotencyKey().isPresent() && subscriptions.values().stream()
                    .flatMap(other -> other.getPayments().stream())
                    .anyMatch(other -> !other.getId().equals(payment.getId())
                            && Objects.equals(other.getIdempotencyKey(), payment.getIdempotencyKey()));
            if (keyTaken) {
                throw new DataIntegrityViolationException("payments_idempotency_key_key");
            }
        }
        subscriptions.put(subscription.getId(), copy(subscription));
        return subscription;
    }

    public Subscription stored(UUID id) {
        return findById(id).orElseThrow();
    }

    private static Subscription copy(Subscription subscription) {
        return new Subscription(subscription.getId(), subscription.getOwnerId(), subscription.getBuildingId(),
                subscription.getPlan(), subscription.getStatus(), subscription.getStartsAt().orElse(null),
                subscription.getExpiresAt().orElse(null), subscription.getCreatedAt(), subscription.getPayments());
    }
}
