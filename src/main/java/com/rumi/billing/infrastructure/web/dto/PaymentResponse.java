package com.rumi.billing.infrastructure.web.dto;

import com.rumi.billing.domain.model.CardBrand;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "PaymentResponse", description = "A payment attempt. Only the brand and last four digits of the card are kept")
public record PaymentResponse(
        @Schema(description = "Id of the payment", example = "1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f")
        UUID id,
        @Schema(description = "Id of the subscription paid", example = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
        UUID subscriptionId,
        @Schema(description = "Amount charged, IGV included", example = "199.00")
        BigDecimal amount,
        @Schema(description = "ISO 4217 currency", example = "PEN")
        String currency,
        @Schema(description = "Outcome of the attempt", example = "APPROVED")
        PaymentStatus status,
        @Schema(description = "Brand of the card", example = "VISA")
        CardBrand cardBrand,
        @Schema(description = "Last four digits of the card", example = "1111")
        String cardLast4,
        @Schema(description = "When the payment was attempted", example = "2026-11-21T15:05:00Z")
        Instant paidAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getSubscriptionId(), payment.getAmount().amount(),
                payment.getAmount().currency(), payment.getStatus(), payment.getCardBrand(), payment.getCardLast4(),
                payment.getCreatedAt());
    }
}
