package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.rumi.billing.domain.model.BillingFixtures.ANNUAL;
import static com.rumi.billing.domain.model.BillingFixtures.BUILDING_ID;
import static com.rumi.billing.domain.model.BillingFixtures.MONTHLY;
import static com.rumi.billing.domain.model.BillingFixtures.NOW;
import static com.rumi.billing.domain.model.BillingFixtures.OWNER_ID;
import static com.rumi.billing.domain.model.BillingFixtures.SUBSCRIPTION_ID;
import static com.rumi.billing.domain.model.BillingFixtures.approvedPayment;
import static com.rumi.billing.domain.model.BillingFixtures.rejectedPayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static Subscription chosen(Plan plan) {
        return Subscription.choose(SUBSCRIPTION_ID, OWNER_ID, BUILDING_ID, plan, NOW);
    }

    @Test
    void choosingAPlanCreatesASubscriptionPendingPayment() {
        Subscription subscription = chosen(MONTHLY);

        assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(subscription.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(subscription.getBuildingId()).isEqualTo(BUILDING_ID);
        assertThat(subscription.getPlan()).isSameAs(MONTHLY);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(subscription.getStartsAt()).isEmpty();
        assertThat(subscription.getExpiresAt()).isEmpty();
        assertThat(subscription.getCreatedAt()).isEqualTo(NOW);
        assertThat(subscription.getPayments()).isEmpty();
        assertThat(subscription.isOpen()).isTrue();
    }

    @Test
    void theFirstApprovedPaymentActivatesTheSubscriptionForOneCycle() {
        Subscription subscription = chosen(MONTHLY);
        Instant paidAt = NOW.plus(Duration.ofHours(1));
        Payment payment = approvedPayment(subscription, paidAt);

        subscription.registerApprovedPayment(payment, paidAt);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartsAt()).contains(paidAt);
        assertThat(subscription.getExpiresAt()).contains(Instant.parse("2026-02-28T16:00:00Z"));
        assertThat(subscription.getPayments()).containsExactly(payment);
    }

    @Test
    void anAnnualPlanIsActiveForOneYear() {
        Subscription subscription = chosen(ANNUAL);

        subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW);

        assertThat(subscription.getExpiresAt()).contains(Instant.parse("2027-01-31T15:00:00Z"));
    }

    @Test
    void aPaymentOnAnActiveSubscriptionRenewsItFromTheCurrentExpiry() {
        Subscription subscription = chosen(MONTHLY);
        subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW);
        Instant renewedAt = NOW.plus(Duration.ofDays(10));

        subscription.registerApprovedPayment(approvedPayment(subscription, renewedAt), renewedAt);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartsAt()).contains(NOW);
        assertThat(subscription.getExpiresAt()).contains(Instant.parse("2026-03-28T15:00:00Z"));
        assertThat(subscription.getPayments()).hasSize(2);
    }

    @Test
    void aPaymentOnAnExpiredSubscriptionRestartsItFromNow() {
        Subscription subscription = chosen(MONTHLY);
        subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW);
        Instant later = Instant.parse("2026-06-10T09:00:00Z");

        subscription.registerApprovedPayment(approvedPayment(subscription, later), later);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartsAt()).contains(later);
        assertThat(subscription.getExpiresAt()).contains(Instant.parse("2026-07-10T09:00:00Z"));
    }

    @Test
    void aPaymentOnASubscriptionStoredAsExpiredRestartsItFromNow() {
        Subscription subscription = stored(SubscriptionStatus.EXPIRED, NOW, Instant.parse("2026-02-28T15:00:00Z"));
        Instant later = Instant.parse("2026-06-10T09:00:00Z");

        subscription.registerApprovedPayment(approvedPayment(subscription, later), later);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartsAt()).contains(later);
        assertThat(subscription.getExpiresAt()).contains(Instant.parse("2026-07-10T09:00:00Z"));
    }

    @Test
    void aRejectedPaymentIsRecordedAndChangesNothingElse() {
        Subscription subscription = chosen(MONTHLY);
        Payment rejected = rejectedPayment(subscription, NOW);

        subscription.registerRejectedPayment(rejected);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(subscription.getStartsAt()).isEmpty();
        assertThat(subscription.getExpiresAt()).isEmpty();
        assertThat(subscription.getPayments()).containsExactly(rejected);
    }

    @Test
    void aCancelledSubscriptionCannotBePaid() {
        Subscription subscription = stored(SubscriptionStatus.CANCELLED, null, null);

        assertThat(subscription.isPayableAt(NOW)).isFalse();
        assertThatThrownBy(() -> subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A cancelled subscription cannot be paid");
        assertThatThrownBy(() -> subscription.registerRejectedPayment(rejectedPayment(subscription, NOW)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subscription.getPayments()).isEmpty();
    }

    @Test
    void statusAtReportsExpiredFromTheExpiryInstantOn() {
        Subscription subscription = chosen(MONTHLY);
        subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW);
        Instant expiresAt = subscription.getExpiresAt().orElseThrow();

        assertThat(subscription.statusAt(expiresAt.minusSeconds(1))).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.statusAt(expiresAt)).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(chosen(MONTHLY).statusAt(NOW.plus(Duration.ofDays(400))))
                .isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
    }

    @Test
    void expireIfDueStoresTheExpiredStatusOnlyAfterTheExpiry() {
        Subscription subscription = chosen(MONTHLY);
        subscription.registerApprovedPayment(approvedPayment(subscription, NOW), NOW);
        Instant expiresAt = subscription.getExpiresAt().orElseThrow();

        assertThat(subscription.expireIfDue(expiresAt.minusSeconds(1))).isFalse();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.isOpen()).isTrue();

        assertThat(subscription.expireIfDue(expiresAt)).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(subscription.isOpen()).isFalse();
        assertThat(chosen(MONTHLY).expireIfDue(NOW.plus(Duration.ofDays(400)))).isFalse();
    }

    @Test
    void rejectsAPaymentOfAnotherSubscriptionOrWithTheWrongStatusOrAmount() {
        Subscription subscription = chosen(MONTHLY);
        Subscription other = Subscription.choose(UUID.randomUUID(), OWNER_ID, UUID.randomUUID(), MONTHLY, NOW);
        Subscription annual = Subscription.choose(UUID.randomUUID(), OWNER_ID, UUID.randomUUID(), ANNUAL, NOW);

        assertThatThrownBy(() -> subscription.registerApprovedPayment(approvedPayment(other, NOW), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The payment belongs to another subscription");
        assertThatThrownBy(() -> subscription.registerApprovedPayment(rejectedPayment(subscription, NOW), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The payment is not approved");
        assertThatThrownBy(() -> subscription.registerRejectedPayment(approvedPayment(subscription, NOW)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The payment is not rejected");
        Payment annualPrice = Payment.approved(UUID.randomUUID(), subscription.getId(), ANNUAL.getPrice(),
                CardBrand.VISA, "1111", "sim_ref", null, NOW);
        assertThatThrownBy(() -> subscription.registerApprovedPayment(annualPrice, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The payment amount must be the price of the plan");
        assertThat(annual.getPayments()).isEmpty();
    }

    @Test
    void ownershipIsCheckedByOwnerId() {
        Subscription subscription = chosen(MONTHLY);

        assertThat(subscription.belongsTo(OWNER_ID)).isTrue();
        assertThat(subscription.belongsTo(UUID.randomUUID())).isFalse();
    }

    private static Subscription stored(SubscriptionStatus status, Instant startsAt, Instant expiresAt) {
        return new Subscription(SUBSCRIPTION_ID, OWNER_ID, BUILDING_ID, MONTHLY, status, startsAt, expiresAt, NOW,
                List.of());
    }
}
