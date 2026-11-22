package com.rumi.billing.application;

import com.rumi.billing.domain.model.CardDetails;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.Plan;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import com.rumi.billing.domain.repository.PlanRepository;
import com.rumi.billing.domain.repository.SubscriptionRepository;
import com.rumi.billing.domain.service.ChargeRequest;
import com.rumi.billing.domain.service.DeclineReason;
import com.rumi.billing.domain.service.GatewayResult;
import com.rumi.billing.domain.service.PaymentGatewayClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases of Billing for the administrator of a building: list the plans and choose one (US05), read the
 * subscription and its payments (US07) and pay it online (US24). The owner of every subscription is the
 * administrator who chose it; anyone else gets "not found".
 */
@Service
public class BillingApplicationService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGatewayClient paymentGateway;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public BillingApplicationService(PlanRepository planRepository, SubscriptionRepository subscriptionRepository,
                                     PaymentGatewayClient paymentGateway, Clock clock,
                                     TransactionTemplate transactions) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    /**
     * Creates the subscription of the building as {@code PENDING_PAYMENT}. A building with a subscription pending
     * payment or still active cannot choose again; an active one whose expiry has passed is stored as
     * {@code EXPIRED} first, and the building may then choose.
     */
    @Transactional
    public Subscription chooseSubscription(UUID ownerId, UUID buildingId, String planCode) {
        Plan plan = planRepository.findByCode(planCode).orElseThrow(PlanNotFoundException::new);
        Instant now = clock.instant();
        Optional<Subscription> open = subscriptionRepository.findOpenByBuildingIdForUpdate(buildingId);
        if (open.isPresent()) {
            Subscription current = open.get();
            if (!current.expireIfDue(now)) {
                throw new BuildingAlreadySubscribedException();
            }
            subscriptionRepository.save(current);
        }
        try {
            return subscriptionRepository.save(Subscription.choose(UUID.randomUUID(), ownerId, buildingId, plan, now));
        } catch (DataIntegrityViolationException concurrentChoice) {
            throw new BuildingAlreadySubscribedException();
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionDetails getSubscription(UUID ownerId, UUID subscriptionId) {
        Subscription subscription = ownedSubscription(ownerId, subscriptionId);
        return new SubscriptionDetails(subscription, subscription.statusAt(clock.instant()));
    }

    /**
     * Every payment attempt of the subscription, approved or rejected, newest first.
     */
    @Transactional(readOnly = true)
    public List<Payment> listPayments(UUID ownerId, UUID subscriptionId) {
        List<Payment> payments = new ArrayList<>(ownedSubscription(ownerId, subscriptionId).getPayments());
        Collections.reverse(payments);
        payments.sort(Comparator.comparing(Payment::getCreatedAt).reversed());
        return payments;
    }

    /**
     * Charges the price of the plan to the card and, when the gateway approves, activates or renews the
     * subscription. A declined charge is stored as {@code REJECTED} and then reported with
     * {@link PaymentRejectedException}, after its transaction has committed. A repeated Idempotency-Key answers
     * with the payment stored for it and charges nothing.
     * <p>
     * The whole attempt is one transaction that holds the lock on the subscription row, so payments of the same
     * subscription run one after the other. That is fine for the simulated gateway; with a real remote gateway the
     * charge should move out of the transaction (reserve the attempt, commit, charge, then record the result in a
     * second transaction) and the Idempotency-Key should be passed on to the gateway.
     */
    public PaymentReceipt pay(UUID ownerId, UUID subscriptionId, CardInput cardInput, String idempotencyKey) {
        CardDetails card = cardInput.toCardDetails(clock.instant());
        PaymentOutcome outcome;
        try {
            outcome = transactions.execute(status -> attemptPayment(ownerId, subscriptionId, card, idempotencyKey));
        } catch (DataIntegrityViolationException concurrentRequest) {
            // Another request stored the same Idempotency-Key first: answer with its payment.
            if (idempotencyKey == null) {
                throw concurrentRequest;
            }
            outcome = transactions.execute(status -> {
                Subscription subscription = ownedSubscription(ownerId, subscriptionId);
                return storedOutcome(subscription, idempotencyKey).orElseThrow(() -> concurrentRequest);
            });
        }
        if (!outcome.payment().isApproved()) {
            throw new PaymentRejectedException(outcome.payment(), outcome.declineReason());
        }
        return new PaymentReceipt(outcome.payment(), outcome.created());
    }

    private PaymentOutcome attemptPayment(UUID ownerId, UUID subscriptionId, CardDetails card, String idempotencyKey) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .filter(found -> found.belongsTo(ownerId))
                .orElseThrow(SubscriptionNotFoundException::new);
        if (idempotencyKey != null) {
            Optional<PaymentOutcome> stored = storedOutcome(subscription, idempotencyKey);
            if (stored.isPresent()) {
                return stored.get();
            }
        }
        Instant now = clock.instant();
        requirePayable(subscription, now);

        Plan plan = subscription.getPlan();
        GatewayResult result = paymentGateway.charge(new ChargeRequest(plan.getPrice(), card));
        Payment payment = result.approved()
                ? Payment.approved(UUID.randomUUID(), subscription.getId(), plan.getPrice(), card.brand(),
                        card.last4(), result.reference(), idempotencyKey, now)
                : Payment.rejected(UUID.randomUUID(), subscription.getId(), plan.getPrice(), card.brand(),
                        card.last4(), result.reference(), idempotencyKey, now);
        if (payment.isApproved()) {
            subscription.registerApprovedPayment(payment, now);
        } else {
            subscription.registerRejectedPayment(payment);
        }
        subscriptionRepository.save(subscription);
        return new PaymentOutcome(payment, true, result.declineReason());
    }

    private Optional<PaymentOutcome> storedOutcome(Subscription subscription, String idempotencyKey) {
        return subscriptionRepository.findPaymentByIdempotencyKey(idempotencyKey).map(stored -> {
            if (!stored.getSubscriptionId().equals(subscription.getId())) {
                throw new IdempotencyKeyConflictException();
            }
            return new PaymentOutcome(stored, false, Optional.empty());
        });
    }

    private void requirePayable(Subscription subscription, Instant now) {
        if (!subscription.isPayableAt(now)) {
            throw new SubscriptionNotPayableException(SubscriptionNotPayableException.CANCELLED);
        }
        if (subscription.statusAt(now) == SubscriptionStatus.EXPIRED) {
            boolean replaced = subscriptionRepository.findOpenByBuildingIdForUpdate(subscription.getBuildingId())
                    .filter(open -> !open.getId().equals(subscription.getId()))
                    .isPresent();
            if (replaced) {
                throw new SubscriptionNotPayableException(SubscriptionNotPayableException.REPLACED);
            }
        }
    }

    private Subscription ownedSubscription(UUID ownerId, UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .filter(subscription -> subscription.belongsTo(ownerId))
                .orElseThrow(SubscriptionNotFoundException::new);
    }

    private record PaymentOutcome(Payment payment, boolean created, Optional<DeclineReason> declineReason) {
    }
}
