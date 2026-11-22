package com.rumi.billing.domain.repository;

import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.Subscription;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads and saves the {@link Subscription} aggregate together with its payments, oldest payment first.
 */
public interface SubscriptionRepository {

    Optional<Subscription> findById(UUID id);

    /**
     * Loads the subscription and locks its row until the transaction ends, so that two payments of the same
     * subscription run one after the other.
     */
    Optional<Subscription> findByIdForUpdate(UUID id);

    /**
     * The subscription of the building whose stored status is open ({@code PENDING_PAYMENT} or {@code ACTIVE}),
     * locked until the transaction ends.
     */
    Optional<Subscription> findOpenByBuildingIdForUpdate(UUID buildingId);

    Optional<Payment> findPaymentByIdempotencyKey(String idempotencyKey);

    /**
     * Saves the subscription and inserts its new payments. It writes at once, so that a second open subscription for
     * the same building or a repeated idempotency key fails here, on the unique indexes.
     */
    Subscription save(Subscription subscription);
}
