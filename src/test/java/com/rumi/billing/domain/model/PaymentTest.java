package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.rumi.billing.domain.model.BillingFixtures.MONTHLY;
import static com.rumi.billing.domain.model.BillingFixtures.NOW;
import static com.rumi.billing.domain.model.BillingFixtures.SUBSCRIPTION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void anApprovedPaymentKeepsOnlyTheBrandAndTheLastFourDigits() {
        UUID id = UUID.randomUUID();

        Payment payment = Payment.approved(id, SUBSCRIPTION_ID, MONTHLY.getPrice(), CardBrand.VISA, "1111",
                "sim_ref", "key-1", NOW);

        assertThat(payment.getId()).isEqualTo(id);
        assertThat(payment.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(payment.getAmount()).isEqualTo(MONTHLY.getPrice());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.isApproved()).isTrue();
        assertThat(payment.getCardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(payment.getCardLast4()).isEqualTo("1111");
        assertThat(payment.getGatewayReference()).isEqualTo("sim_ref");
        assertThat(payment.getIdempotencyKey()).contains("key-1");
        assertThat(payment.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void aRejectedPaymentIsNotApprovedAndMayHaveNoIdempotencyKey() {
        Payment payment = Payment.rejected(UUID.randomUUID(), SUBSCRIPTION_ID, MONTHLY.getPrice(), CardBrand.VISA,
                "0002", "sim_ref", null, NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(payment.isApproved()).isFalse();
        assertThat(payment.getIdempotencyKey()).isEmpty();
    }

    @Test
    void rejectsLastDigitsThatAreNotFourDigits() {
        assertThatThrownBy(() -> Payment.approved(UUID.randomUUID(), SUBSCRIPTION_ID, MONTHLY.getPrice(),
                CardBrand.VISA, "4111111111111111", "sim_ref", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card last digits must be exactly 4 digits");
    }

    @Test
    void rejectsAnIdempotencyKeyLongerThan64Characters() {
        assertThatThrownBy(() -> Payment.approved(UUID.randomUUID(), SUBSCRIPTION_ID, MONTHLY.getPrice(),
                CardBrand.VISA, "1111", "sim_ref", "k".repeat(65), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency key must have between 1 and 64 characters");
    }

    @Test
    void requiresAGatewayReference() {
        assertThatThrownBy(() -> Payment.approved(UUID.randomUUID(), SUBSCRIPTION_ID, MONTHLY.getPrice(),
                CardBrand.VISA, "1111", " ", null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Gateway reference is required");
    }
}
