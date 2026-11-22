package com.rumi.billing.infrastructure.web.dto;

import com.rumi.billing.application.SubscriptionDetails;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.domain.model.SubscriptionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "SubscriptionResponse", description = "The subscription of a building")
public record SubscriptionResponse(
        @Schema(description = "Id of the subscription", example = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
        UUID id,
        @Schema(description = "Id of the building", example = "3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b")
        UUID buildingId,
        PlanResponse plan,
        @Schema(description = "Status as of now: an active subscription past its expiry is EXPIRED",
                example = "ACTIVE")
        SubscriptionStatus status,
        @Schema(description = "When the current period started; null until the first approved payment",
                example = "2026-11-21T15:05:00Z", nullable = true)
        Instant startsAt,
        @Schema(description = "When the subscription expires; null until the first approved payment",
                example = "2026-12-21T15:05:00Z", nullable = true)
        Instant expiresAt,
        @Schema(description = "When the plan was chosen", example = "2026-11-21T15:00:00Z")
        Instant createdAt
) {

    public static SubscriptionResponse from(Subscription subscription, SubscriptionStatus status) {
        return new SubscriptionResponse(subscription.getId(), subscription.getBuildingId(),
                PlanResponse.from(subscription.getPlan()), status, subscription.getStartsAt().orElse(null),
                subscription.getExpiresAt().orElse(null), subscription.getCreatedAt());
    }

    public static SubscriptionResponse from(SubscriptionDetails details) {
        return from(details.subscription(), details.status());
    }
}
