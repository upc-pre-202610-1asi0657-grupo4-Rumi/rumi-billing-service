package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.Money;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import com.rumi.billing.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JpaSubscriptionRepository implements SubscriptionRepository {

    private static final Set<SubscriptionStatus> OPEN = EnumSet.of(SubscriptionStatus.PENDING_PAYMENT,
            SubscriptionStatus.ACTIVE);

    private final SpringDataSubscriptionRepository subscriptions;
    private final SpringDataPaymentRepository payments;
    private final SpringDataPlanRepository plans;

    public JpaSubscriptionRepository(SpringDataSubscriptionRepository subscriptions,
                                     SpringDataPaymentRepository payments, SpringDataPlanRepository plans) {
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.plans = plans;
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        return subscriptions.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByIdForUpdate(UUID id) {
        return subscriptions.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findOpenByBuildingIdForUpdate(UUID buildingId) {
        return subscriptions.findByBuildingIdAndStatusInForUpdate(buildingId, OPEN).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findPaymentByIdempotencyKey(String idempotencyKey) {
        return payments.findByIdempotencyKey(idempotencyKey).map(JpaSubscriptionRepository::toDomain);
    }

    /**
     * Payments never change once made, so only the ones not stored yet are inserted.
     */
    @Override
    public Subscription save(Subscription subscription) {
        subscriptions.saveAndFlush(toEntity(subscription));
        Set<UUID> stored = payments.findIdsBySubscriptionId(subscription.getId());
        subscription.getPayments().stream()
                .filter(payment -> !stored.contains(payment.getId()))
                .forEach(payment -> payments.saveAndFlush(toEntity(payment)));
        return subscription;
    }

    private SubscriptionJpaEntity toEntity(Subscription subscription) {
        return new SubscriptionJpaEntity(
                subscription.getId(),
                subscription.getOwnerId(),
                subscription.getBuildingId(),
                plans.getReferenceById(subscription.getPlan().getId()),
                subscription.getStatus(),
                subscription.getStartsAt().orElse(null),
                subscription.getExpiresAt().orElse(null),
                subscription.getCreatedAt()
        );
    }

    private Subscription toDomain(SubscriptionJpaEntity entity) {
        return new Subscription(
                entity.getId(),
                entity.getOwnerId(),
                entity.getBuildingId(),
                JpaPlanRepository.toDomain(entity.getPlan()),
                entity.getStatus(),
                entity.getStartsAt(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                payments.findBySubscriptionIdOrderByCreatedAtAscIdAsc(entity.getId()).stream()
                        .map(JpaSubscriptionRepository::toDomain)
                        .toList()
        );
    }

    private static PaymentJpaEntity toEntity(Payment payment) {
        return new PaymentJpaEntity(
                payment.getId(),
                payment.getSubscriptionId(),
                payment.getAmount().amount(),
                payment.getAmount().currency(),
                payment.getStatus(),
                payment.getCardBrand(),
                payment.getCardLast4(),
                payment.getGatewayReference(),
                payment.getIdempotencyKey().orElse(null),
                payment.getCreatedAt()
        );
    }

    private static Payment toDomain(PaymentJpaEntity entity) {
        return new Payment(
                entity.getId(),
                entity.getSubscriptionId(),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getStatus(),
                entity.getCardBrand(),
                entity.getCardLast4(),
                entity.getGatewayReference(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt()
        );
    }
}
