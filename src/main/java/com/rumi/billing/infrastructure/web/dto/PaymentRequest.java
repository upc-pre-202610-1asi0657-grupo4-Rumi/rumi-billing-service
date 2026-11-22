package com.rumi.billing.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "PaymentRequest", description = "Online payment of a subscription")
public record PaymentRequest(
        @Schema(description = "Id of the subscription to pay", example = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
        @NotNull
        UUID subscriptionId,
        @NotNull @Valid
        CardRequest card
) {

    @Override
    public String toString() {
        return "PaymentRequest[subscriptionId=" + subscriptionId + "]";
    }
}
