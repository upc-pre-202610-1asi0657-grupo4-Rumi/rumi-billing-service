package com.rumi.billing.infrastructure.web;

import com.rumi.billing.domain.model.BillingFixtures;
import com.rumi.billing.domain.model.CardBrand;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.PaymentStatus;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class WebFixtures {

    static final UUID OWNER_ID = BillingFixtures.OWNER_ID;
    static final UUID BUILDING_ID = BillingFixtures.BUILDING_ID;
    static final UUID SUBSCRIPTION_ID = BillingFixtures.SUBSCRIPTION_ID;
    static final UUID PAYMENT_ID = UUID.fromString("1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f");
    static final Instant CREATED_AT = Instant.parse("2026-11-21T15:00:00Z");
    static final Instant PAID_AT = Instant.parse("2026-11-21T15:05:00Z");

    private WebFixtures() {
    }

    static MockHttpServletRequestBuilder asAdministrator(MockHttpServletRequestBuilder request) {
        return request.header("X-User-Id", OWNER_ID.toString()).header("X-Role", "ADMINISTRATOR");
    }

    static Subscription pending() {
        return new Subscription(SUBSCRIPTION_ID, OWNER_ID, BUILDING_ID, BillingFixtures.MONTHLY,
                SubscriptionStatus.PENDING_PAYMENT, null, null, CREATED_AT, List.of());
    }

    static Payment payment(PaymentStatus status) {
        return new Payment(PAYMENT_ID, SUBSCRIPTION_ID, BillingFixtures.MONTHLY.getPrice(), status, CardBrand.VISA,
                "1111", "sim_3b0c1f8e-2a4d-4b6e-9c8f-7a6b5c4d3e2f", null, PAID_AT);
    }

    static Subscription active() {
        return new Subscription(SUBSCRIPTION_ID, OWNER_ID, BUILDING_ID, BillingFixtures.MONTHLY,
                SubscriptionStatus.ACTIVE, PAID_AT, Instant.parse("2026-12-21T15:05:00Z"), CREATED_AT,
                List.of(payment(PaymentStatus.APPROVED)));
    }
}
