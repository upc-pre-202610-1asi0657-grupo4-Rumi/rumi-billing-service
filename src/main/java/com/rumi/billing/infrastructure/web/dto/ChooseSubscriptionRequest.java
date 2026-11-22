package com.rumi.billing.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(name = "ChooseSubscriptionRequest", description = "The plan chosen for a building")
public record ChooseSubscriptionRequest(
        @Schema(description = "Id of the building in the Building service", example = "3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b")
        @NotNull
        UUID buildingId,
        @Schema(description = "Code of the plan", example = "MONTHLY", allowableValues = {"MONTHLY", "ANNUAL"})
        @NotBlank @Size(max = 20)
        String planCode
) {
}
