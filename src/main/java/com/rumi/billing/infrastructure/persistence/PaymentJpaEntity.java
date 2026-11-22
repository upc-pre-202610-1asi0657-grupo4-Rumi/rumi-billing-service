package com.rumi.billing.infrastructure.persistence;

import com.rumi.billing.domain.model.CardBrand;
import com.rumi.billing.domain.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Row of a payment attempt. There is no column for the card number, the CVV or the holder name.
 */
@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", nullable = false, length = 12)
    private CardBrand cardBrand;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "card_last4", nullable = false, length = 4)
    private String cardLast4;

    @Column(name = "gateway_reference", nullable = false, length = 64)
    private String gatewayReference;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentJpaEntity() {
    }

    public PaymentJpaEntity(UUID id, UUID subscriptionId, BigDecimal amount, String currency, PaymentStatus status,
                            CardBrand cardBrand, String cardLast4, String gatewayReference, String idempotencyKey,
                            Instant createdAt) {
        this.id = id;
        this.subscriptionId = subscriptionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.cardBrand = cardBrand;
        this.cardLast4 = cardLast4;
        this.gatewayReference = gatewayReference;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
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

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
