package com.rumi.billing.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One payment attempt for a {@link Subscription}, which is its aggregate root (US24). Only the brand and the last four
 * digits of the card are kept; the card number, the CVV and the holder name never reach this class.
 */
public final class Payment {

    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;
    public static final int MAX_GATEWAY_REFERENCE_LENGTH = 64;

    private final UUID id;
    private final UUID subscriptionId;
    private final Money amount;
    private final PaymentStatus status;
    private final CardBrand cardBrand;
    private final String cardLast4;
    private final String gatewayReference;
    private final String idempotencyKey;
    private final Instant createdAt;

    public Payment(UUID id, UUID subscriptionId, Money amount, PaymentStatus status, CardBrand cardBrand,
                   String cardLast4, String gatewayReference, String idempotencyKey, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Payment id is required");
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "Payment subscription id is required");
        this.amount = Objects.requireNonNull(amount, "Payment amount is required");
        this.status = Objects.requireNonNull(status, "Payment status is required");
        this.cardBrand = Objects.requireNonNull(cardBrand, "Card brand is required");
        this.cardLast4 = requireLast4(cardLast4);
        this.gatewayReference = requireGatewayReference(gatewayReference);
        this.idempotencyKey = requireIdempotencyKey(idempotencyKey);
        this.createdAt = Objects.requireNonNull(createdAt, "Payment time is required");
    }

    public static Payment approved(UUID id, UUID subscriptionId, Money amount, CardBrand cardBrand, String cardLast4,
                                   String gatewayReference, String idempotencyKey, Instant createdAt) {
        return new Payment(id, subscriptionId, amount, PaymentStatus.APPROVED, cardBrand, cardLast4, gatewayReference,
                idempotencyKey, createdAt);
    }

    public static Payment rejected(UUID id, UUID subscriptionId, Money amount, CardBrand cardBrand, String cardLast4,
                                   String gatewayReference, String idempotencyKey, Instant createdAt) {
        return new Payment(id, subscriptionId, amount, PaymentStatus.REJECTED, cardBrand, cardLast4, gatewayReference,
                idempotencyKey, createdAt);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public Money getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public CardBrand getCardBrand() {
        return cardBrand;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public Optional<String> getIdempotencyKey() {
        return Optional.ofNullable(idempotencyKey);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Payment[id=" + id + ", subscriptionId=" + subscriptionId + ", status=" + status + "]";
    }

    private static String requireLast4(String cardLast4) {
        if (cardLast4 == null || !cardLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Card last digits must be exactly 4 digits");
        }
        return cardLast4;
    }

    private static String requireGatewayReference(String gatewayReference) {
        if (gatewayReference == null || gatewayReference.isBlank()) {
            throw new IllegalArgumentException("Gateway reference is required");
        }
        if (gatewayReference.length() > MAX_GATEWAY_REFERENCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Gateway reference must have at most " + MAX_GATEWAY_REFERENCE_LENGTH + " characters");
        }
        return gatewayReference;
    }

    private static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            throw new IllegalArgumentException(
                    "Idempotency key must have between 1 and " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters");
        }
        return idempotencyKey;
    }
}
