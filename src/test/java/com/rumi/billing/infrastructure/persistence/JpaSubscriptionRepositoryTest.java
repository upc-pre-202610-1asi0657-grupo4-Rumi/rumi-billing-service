package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.PostgresTestContainer;
import com.rumi.billing.domain.model.CardBrand;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.PaymentStatus;
import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PostgresTestContainer.class, JpaPlanRepository.class, JpaSubscriptionRepository.class})
class JpaSubscriptionRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f");
    private static final Instant NOW = Instant.parse("2026-01-31T15:00:00Z");

    @Autowired
    private JpaPlanRepository plans;

    @Autowired
    private JpaSubscriptionRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Plan monthly;
    private UUID buildingId;

    @BeforeEach
    void loadThePlan() {
        monthly = plans.findByCode("MONTHLY").orElseThrow();
        buildingId = UUID.randomUUID();
    }

    @Test
    void savesAndReadsASubscriptionWithItsPayments() {
        Subscription subscription = repository.save(chosen(buildingId));
        Payment rejected = payment(subscription, PaymentStatus.REJECTED, "0002", null, NOW);
        subscription.registerRejectedPayment(rejected);
        Payment approved = payment(subscription, PaymentStatus.APPROVED, "1111", "key-" + UUID.randomUUID(),
                NOW.plusSeconds(60));
        subscription.registerApprovedPayment(approved, NOW.plusSeconds(60));
        repository.save(subscription);

        assertThat(repository.findById(subscription.getId())).hasValueSatisfying(found -> {
            assertThat(found.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(found.getBuildingId()).isEqualTo(buildingId);
            assertThat(found.getPlan().getCode()).isEqualTo("MONTHLY");
            assertThat(found.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(found.getStartsAt()).contains(NOW.plusSeconds(60));
            assertThat(found.getExpiresAt()).contains(Instant.parse("2026-02-28T15:01:00Z"));
            assertThat(found.getCreatedAt()).isEqualTo(NOW);
            assertThat(found.getPayments()).extracting(Payment::getId).containsExactly(rejected.getId(), approved.getId());
            Payment stored = found.getPayments().get(1);
            assertThat(stored.getStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(stored.getAmount().amount()).isEqualByComparingTo("149.00");
            assertThat(stored.getAmount().currency()).isEqualTo("PEN");
            assertThat(stored.getCardBrand()).isEqualTo(CardBrand.VISA);
            assertThat(stored.getCardLast4()).isEqualTo("1111");
            assertThat(stored.getGatewayReference()).isEqualTo(approved.getGatewayReference());
            assertThat(stored.getIdempotencyKey()).isEqualTo(approved.getIdempotencyKey());
            assertThat(stored.getCreatedAt()).isEqualTo(NOW.plusSeconds(60));
        });
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void savingTheSameSubscriptionTwiceDoesNotDuplicateItsPayments() {
        Subscription subscription = repository.save(chosen(buildingId));
        subscription.registerApprovedPayment(payment(subscription, PaymentStatus.APPROVED, "1111", null, NOW), NOW);
        repository.save(subscription);
        repository.save(subscription);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payments WHERE subscription_id = ?",
                Integer.class, subscription.getId())).isEqualTo(1);
    }

    @Test
    void keepsMoneyWithTwoDecimals() {
        Subscription subscription = repository.save(chosen(buildingId));
        subscription.registerApprovedPayment(payment(subscription, PaymentStatus.APPROVED, "1111", null, NOW), NOW);
        repository.save(subscription);

        BigDecimal stored = jdbcTemplate.queryForObject("SELECT amount FROM payments WHERE subscription_id = ?",
                BigDecimal.class, subscription.getId());
        assertThat(stored).isEqualTo(new BigDecimal("149.00"));
    }

    @Test
    void findsTheOpenSubscriptionOfABuildingForUpdate() {
        Subscription subscription = repository.save(chosen(buildingId));

        assertThat(repository.findOpenByBuildingIdForUpdate(buildingId)).map(Subscription::getId)
                .contains(subscription.getId());
        assertThat(repository.findByIdForUpdate(subscription.getId())).isPresent();
        assertThat(repository.findOpenByBuildingIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void aClosedSubscriptionIsNotTheOpenOneOfItsBuilding() {
        repository.save(stored(buildingId, SubscriptionStatus.CANCELLED));
        repository.save(stored(buildingId, SubscriptionStatus.EXPIRED));

        assertThat(repository.findOpenByBuildingIdForUpdate(buildingId)).isEmpty();
    }

    @Test
    void storesTheExpiredStatus() {
        Subscription subscription = repository.save(chosen(buildingId));
        subscription.registerApprovedPayment(payment(subscription, PaymentStatus.APPROVED, "1111", null, NOW), NOW);
        repository.save(subscription);
        subscription.expireIfDue(NOW.plusSeconds(60L * 60 * 24 * 40));

        repository.save(subscription);

        assertThat(repository.findById(subscription.getId())).map(Subscription::getStatus)
                .contains(SubscriptionStatus.EXPIRED);
    }

    @Test
    void findsAPaymentByItsIdempotencyKey() {
        Subscription subscription = repository.save(chosen(buildingId));
        String key = "key-" + UUID.randomUUID();
        Payment approved = payment(subscription, PaymentStatus.APPROVED, "1111", key, NOW);
        subscription.registerApprovedPayment(approved, NOW);
        repository.save(subscription);

        assertThat(repository.findPaymentByIdempotencyKey(key)).map(Payment::getId).contains(approved.getId());
        assertThat(repository.findPaymentByIdempotencyKey("unknown")).isEmpty();
    }

    @Test
    void anotherOpenSubscriptionIsAllowedOnceTheFirstIsCancelled() {
        repository.save(stored(buildingId, SubscriptionStatus.CANCELLED));

        repository.save(chosen(buildingId));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM subscriptions WHERE building_id = ?",
                Integer.class, buildingId)).isEqualTo(2);
    }

    @Test
    void thePartialUniqueIndexRejectsASecondOpenSubscriptionForTheSameBuilding() {
        repository.save(chosen(buildingId));

        assertThatThrownBy(() -> repository.save(chosen(buildingId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theIdempotencyKeyIsUnique() {
        String key = "key-" + UUID.randomUUID();
        Subscription first = repository.save(chosen(buildingId));
        first.registerApprovedPayment(payment(first, PaymentStatus.APPROVED, "1111", key, NOW), NOW);
        repository.save(first);
        Subscription second = repository.save(chosen(UUID.randomUUID()));
        second.registerApprovedPayment(payment(second, PaymentStatus.APPROVED, "1111", key, NOW), NOW);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Subscription chosen(UUID building) {
        return Subscription.choose(UUID.randomUUID(), OWNER_ID, building, monthly, NOW);
    }

    private Subscription stored(UUID building, SubscriptionStatus status) {
        return new Subscription(UUID.randomUUID(), OWNER_ID, building, monthly, status, NOW,
                NOW.plusSeconds(3600), NOW, List.of());
    }

    private Payment payment(Subscription subscription, PaymentStatus status, String last4, String key, Instant at) {
        return new Payment(UUID.randomUUID(), subscription.getId(), monthly.getPrice(), status, CardBrand.VISA, last4,
                "sim_" + UUID.randomUUID(), key, at);
    }
}
