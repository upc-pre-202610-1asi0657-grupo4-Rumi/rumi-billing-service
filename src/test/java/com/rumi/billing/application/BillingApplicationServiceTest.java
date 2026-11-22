package com.rumi.billing.application;

import com.rumi.billing.MutableClock;
import com.rumi.billing.domain.model.CardBrand;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.PaymentStatus;
import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import com.rumi.billing.domain.service.DeclineReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.rumi.billing.domain.model.BillingFixtures.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-31T15:00:00Z");
    private static final UUID OWNER_ID = UUID.fromString("7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f");
    private static final UUID BUILDING_ID = UUID.fromString("3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b");
    private static final CardInput VISA = new CardInput("4111111111111111", "Ana Torres", 12, 2028, "123");

    private final InMemorySubscriptionRepository subscriptions = new InMemorySubscriptionRepository();
    private final FakePaymentGatewayClient gateway = new FakePaymentGatewayClient();
    private final MutableClock clock = new MutableClock(NOW);
    private BillingApplicationService service;

    @BeforeEach
    void createTheService() {
        service = new BillingApplicationService(new InMemoryPlanRepository(), subscriptions, gateway, clock,
                new TransactionTemplate(new NoOpTransactionManager()));
    }

    @Test
    void listsThePlansCheapestFirst() {
        assertThat(service.listPlans()).extracting(Plan::getCode).containsExactly("MONTHLY", "ANNUAL");
    }

    @Test
    void choosingAPlanStoresASubscriptionPendingPayment() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");

        Subscription stored = subscriptions.stored(subscription.getId());
        assertThat(stored.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(stored.getBuildingId()).isEqualTo(BUILDING_ID);
        assertThat(stored.getPlan()).isEqualTo(MONTHLY);
        assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(stored.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void anUnknownPlanIsNotFound() {
        assertThatThrownBy(() -> service.chooseSubscription(OWNER_ID, BUILDING_ID, "WEEKLY"))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessage(PlanNotFoundException.MESSAGE);
    }

    @Test
    void aBuildingWithASubscriptionPendingPaymentCannotChooseAgain() {
        service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");

        assertThatThrownBy(() -> service.chooseSubscription(OWNER_ID, BUILDING_ID, "ANNUAL"))
                .isInstanceOf(BuildingAlreadySubscribedException.class)
                .hasMessage(BuildingAlreadySubscribedException.MESSAGE);
    }

    @Test
    void aBuildingWithACurrentActiveSubscriptionCannotChooseAgain() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, subscription.getId(), VISA, null);
        clock.advance(Duration.ofDays(27));

        assertThatThrownBy(() -> service.chooseSubscription(OWNER_ID, BUILDING_ID, "ANNUAL"))
                .isInstanceOf(BuildingAlreadySubscribedException.class);
    }

    @Test
    void choosingAPlanForABuildingWhoseSubscriptionHasExpiredStoresItAsExpiredAndCreatesANewOne() {
        Subscription first = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, first.getId(), VISA, null);
        clock.setInstant(Instant.parse("2026-02-28T15:00:00Z"));

        Subscription second = service.chooseSubscription(OWNER_ID, BUILDING_ID, "ANNUAL");

        assertThat(subscriptions.stored(first.getId()).getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(subscriptions.stored(second.getId()).getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(second.getPlan().getCode()).isEqualTo("ANNUAL");
    }

    @Test
    void readsTheSubscriptionOfItsOwnerOnly() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");

        assertThat(service.getSubscription(OWNER_ID, subscription.getId()).status())
                .isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> service.getSubscription(UUID.randomUUID(), subscription.getId()))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessage(SubscriptionNotFoundException.MESSAGE);
        assertThatThrownBy(() -> service.getSubscription(OWNER_ID, UUID.randomUUID()))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void reportsAnActiveSubscriptionPastItsExpiryAsExpiredWithoutStoringIt() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, subscription.getId(), VISA, null);
        clock.setInstant(Instant.parse("2026-03-01T00:00:00Z"));

        SubscriptionDetails details = service.getSubscription(OWNER_ID, subscription.getId());

        assertThat(details.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(subscriptions.stored(subscription.getId()).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void anApprovedPaymentActivatesTheSubscriptionAndChargesThePriceOfThePlan() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");

        PaymentReceipt receipt = service.pay(OWNER_ID, subscription.getId(), VISA, "key-1");

        assertThat(receipt.created()).isTrue();
        Payment payment = receipt.payment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getAmount()).isEqualTo(MONTHLY.getPrice());
        assertThat(payment.getCardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(payment.getCardLast4()).isEqualTo("1111");
        assertThat(payment.getGatewayReference()).isEqualTo("fake_1");
        assertThat(payment.getIdempotencyKey()).contains("key-1");
        assertThat(gateway.charges()).singleElement().satisfies(charge -> {
            assertThat(charge.amount()).isEqualTo(MONTHLY.getPrice());
            assertThat(charge.card().number()).isEqualTo("4111111111111111");
        });
        Subscription stored = subscriptions.stored(subscription.getId());
        assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(stored.getExpiresAt()).contains(Instant.parse("2026-02-28T15:00:00Z"));
    }

    @Test
    void aRejectedPaymentIsStoredListedAndReported() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        gateway.declineCharges();

        assertThatThrownBy(() -> service.pay(OWNER_ID, subscription.getId(), VISA, null))
                .isInstanceOfSatisfying(PaymentRejectedException.class, rejected -> {
                    assertThat(rejected.getMessage()).isEqualTo(PaymentRejectedException.MESSAGE);
                    assertThat(rejected.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
                    assertThat(rejected.payment().getStatus()).isEqualTo(PaymentStatus.REJECTED);
                });

        Subscription stored = subscriptions.stored(subscription.getId());
        assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(service.listPayments(OWNER_ID, subscription.getId()))
                .singleElement().extracting(Payment::getStatus).isEqualTo(PaymentStatus.REJECTED);
    }

    @Test
    void listsThePaymentsNewestFirstForTheOwnerOnly() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, subscription.getId(), VISA, null);
        clock.advance(Duration.ofDays(1));
        service.pay(OWNER_ID, subscription.getId(), VISA, null);

        List<Payment> payments = service.listPayments(OWNER_ID, subscription.getId());

        assertThat(payments).extracting(Payment::getCreatedAt).containsExactly(NOW.plus(Duration.ofDays(1)), NOW);
        assertThatThrownBy(() -> service.listPayments(UUID.randomUUID(), subscription.getId()))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void theSameIdempotencyKeyReturnsTheStoredPaymentAndChargesOnce() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        PaymentReceipt first = service.pay(OWNER_ID, subscription.getId(), VISA, "key-1");

        PaymentReceipt repeated = service.pay(OWNER_ID, subscription.getId(), VISA, "key-1");

        assertThat(repeated.created()).isFalse();
        assertThat(repeated.payment().getId()).isEqualTo(first.payment().getId());
        assertThat(gateway.charges()).hasSize(1);
        assertThat(subscriptions.stored(subscription.getId()).getExpiresAt())
                .contains(Instant.parse("2026-02-28T15:00:00Z"));
    }

    @Test
    void theSameIdempotencyKeyForAnotherSubscriptionConflicts() {
        Subscription first = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        Subscription second = service.chooseSubscription(OWNER_ID, UUID.randomUUID(), "MONTHLY");
        service.pay(OWNER_ID, first.getId(), VISA, "key-1");

        assertThatThrownBy(() -> service.pay(OWNER_ID, second.getId(), VISA, "key-1"))
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage(IdempotencyKeyConflictException.MESSAGE);
        assertThat(gateway.charges()).hasSize(1);
    }

    @Test
    void repeatingTheKeyOfARejectedPaymentReportsTheRejectionAgainWithoutCharging() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        gateway.declineCharges();
        assertThatThrownBy(() -> service.pay(OWNER_ID, subscription.getId(), VISA, "key-1"))
                .isInstanceOf(PaymentRejectedException.class);

        assertThatThrownBy(() -> service.pay(OWNER_ID, subscription.getId(), VISA, "key-1"))
                .isInstanceOf(PaymentRejectedException.class);
        assertThat(gateway.charges()).hasSize(1);
    }

    @Test
    void aKeyStoredByAConcurrentRequestIsAnsweredWithTheStoredPayment() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        Payment concurrent = Payment.approved(UUID.randomUUID(), subscription.getId(), MONTHLY.getPrice(),
                CardBrand.VISA, "1111", "fake_concurrent", "key-1", NOW);
        subscriptions.beforeNextSave(() -> {
            Subscription other = subscriptions.stored(subscription.getId());
            other.registerApprovedPayment(concurrent, NOW);
            subscriptions.save(other);
        });

        PaymentReceipt receipt = service.pay(OWNER_ID, subscription.getId(), VISA, "key-1");

        assertThat(receipt.created()).isFalse();
        assertThat(receipt.payment().getId()).isEqualTo(concurrent.getId());
        assertThat(subscriptions.stored(subscription.getId()).getPayments()).hasSize(1);
    }

    @Test
    void aCancelledSubscriptionCannotBePaid() {
        UUID id = UUID.randomUUID();
        subscriptions.save(new Subscription(id, OWNER_ID, BUILDING_ID, MONTHLY, SubscriptionStatus.CANCELLED, null,
                null, NOW, List.of()));

        assertThatThrownBy(() -> service.pay(OWNER_ID, id, VISA, null))
                .isInstanceOf(SubscriptionNotPayableException.class)
                .hasMessage(SubscriptionNotPayableException.CANCELLED);
        assertThat(gateway.charges()).isEmpty();
    }

    @Test
    void payingAnExpiredSubscriptionReactivatesItFromNow() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, subscription.getId(), VISA, null);
        Instant later = Instant.parse("2026-06-10T09:00:00Z");
        clock.setInstant(later);

        service.pay(OWNER_ID, subscription.getId(), VISA, null);

        Subscription stored = subscriptions.stored(subscription.getId());
        assertThat(stored.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(stored.getStartsAt()).contains(later);
        assertThat(stored.getExpiresAt()).contains(Instant.parse("2026-07-10T09:00:00Z"));
    }

    @Test
    void anExpiredSubscriptionCannotBePaidOnceTheBuildingHasANewOne() {
        Subscription first = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");
        service.pay(OWNER_ID, first.getId(), VISA, null);
        clock.setInstant(Instant.parse("2026-06-10T09:00:00Z"));
        service.chooseSubscription(OWNER_ID, BUILDING_ID, "ANNUAL");

        assertThatThrownBy(() -> service.pay(OWNER_ID, first.getId(), VISA, null))
                .isInstanceOf(SubscriptionNotPayableException.class)
                .hasMessage(SubscriptionNotPayableException.REPLACED);
        assertThat(gateway.charges()).hasSize(1);
    }

    @Test
    void anInvalidCardIsRejectedBeforeAnythingElse() {
        CardInput invalid = new CardInput("4111111111111112", "Ana Torres", 12, 2028, "123");

        assertThatThrownBy(() -> service.pay(OWNER_ID, UUID.randomUUID(), invalid, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card number is not valid");
        assertThat(gateway.charges()).isEmpty();
    }

    @Test
    void payingAnUnknownSubscriptionOrOneOfAnotherOwnerIsNotFound() {
        Subscription subscription = service.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY");

        assertThatThrownBy(() -> service.pay(OWNER_ID, UUID.randomUUID(), VISA, null))
                .isInstanceOf(SubscriptionNotFoundException.class);
        assertThatThrownBy(() -> service.pay(UUID.randomUUID(), subscription.getId(), VISA, "key-1"))
                .isInstanceOf(SubscriptionNotFoundException.class);
        assertThat(gateway.charges()).isEmpty();
    }

    @Test
    void cardInputNeverPrintsTheCard() {
        assertThat(VISA.toString()).doesNotContain("4111111111111111").doesNotContain("123")
                .doesNotContain("Ana Torres");
    }
}
