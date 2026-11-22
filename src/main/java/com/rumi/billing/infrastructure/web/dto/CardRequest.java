package com.rumi.billing.infrastructure.web.dto;

import com.rumi.billing.application.CardInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "CardRequest", description = "The card to charge. It is never stored, logged or returned")
public record CardRequest(
        @Schema(description = "Card number; it must pass the Luhn check. Test cards: 4111111111111111 is approved, "
                + "4000000000000002 is declined", example = "4111111111111111")
        @NotBlank @Size(max = 23)
        String number,
        @Schema(description = "Name printed on the card", example = "Ana Torres")
        @NotBlank @Size(max = 100)
        String holderName,
        @Schema(description = "Expiry month", example = "12")
        @NotNull @Min(1) @Max(12)
        Integer expiryMonth,
        @Schema(description = "Expiry year, four digits", example = "2030")
        @NotNull @Min(2000) @Max(2099)
        Integer expiryYear,
        @Schema(description = "Security code: 3 digits, or 4 for AMEX", example = "123")
        @NotBlank @Pattern(regexp = "\\d{3,4}", message = "must be 3 or 4 digits")
        String cvv
) {

    public CardInput toCardInput() {
        return new CardInput(number, holderName, expiryMonth, expiryYear, cvv);
    }

    /**
     * Never prints the card, so that it cannot leak into logs.
     */
    @Override
    public String toString() {
        return "CardRequest[hidden]";
    }
}
