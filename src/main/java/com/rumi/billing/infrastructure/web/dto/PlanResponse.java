package com.rumi.billing.infrastructure.web.dto;

import com.rumi.billing.domain.model.BillingCycle;
import com.rumi.billing.domain.model.Plan;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "PlanResponse", description = "A subscription plan")
public record PlanResponse(
        @Schema(description = "Id of the plan", example = "0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001")
        UUID id,
        @Schema(description = "Code used to choose the plan", example = "MONTHLY")
        String code,
        @Schema(description = "Name shown to the administrator", example = "Plan mensual")
        String name,
        @Schema(description = "How often the plan is paid", example = "MONTHLY")
        BillingCycle billingCycle,
        @Schema(description = "Price of one cycle", example = "149.00")
        BigDecimal price,
        @Schema(description = "ISO 4217 currency of the price", example = "PEN")
        String currency,
        @Schema(description = "Maximum number of sensors of the building", example = "20")
        int sensorLimit
) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getBillingCycle(),
                plan.getPrice().amount(), plan.getPrice().currency(), plan.getSensorLimit());
    }
}
