package com.rumi.billing.infrastructure.web;

import com.rumi.billing.application.BillingApplicationService;
import com.rumi.billing.domain.model.Subscription;
import com.rumi.billing.infrastructure.web.dto.ChooseSubscriptionRequest;
import com.rumi.billing.infrastructure.web.dto.PaymentResponse;
import com.rumi.billing.infrastructure.web.dto.PlanResponse;
import com.rumi.billing.infrastructure.web.dto.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.rumi.billing.infrastructure.web.ApiDocumentation.FORBIDDEN_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.GATEWAY_NOTE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.PAYMENT_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.SUBSCRIPTION_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.SUBSCRIPTION_NOT_FOUND_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.UNAUTHORIZED_EXAMPLE;

/**
 * Plans and subscriptions of the buildings of the signed-in administrator. The literal paths {@code /plans} and
 * {@code /health} take precedence over {@code /{subscriptionId}}.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription plans (US05) and subscription status (US07)")
@ApiResponse(responseCode = "401", description = "The request did not come through the gateway with a valid token",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(name = "Missing X-User-Id", value = UNAUTHORIZED_EXAMPLE)))
@ApiResponse(responseCode = "403", description = "The signed-in user is not an administrator",
        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(name = "Resident", value = FORBIDDEN_EXAMPLE)))
public class SubscriptionController {

    private final BillingApplicationService billingService;

    public SubscriptionController(BillingApplicationService billingService) {
        this.billingService = billingService;
    }

    @Operation(summary = "List the subscription plans",
            description = "US05. The plans an administrator can choose for a building, cheapest first." + GATEWAY_NOTE)
    @ApiResponse(responseCode = "200", description = "The plans",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = PlanResponse.class)),
                    examples = @ExampleObject(name = "Two plans", value = """
                            [{"id":"0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001","code":"MONTHLY","name":"Plan mensual",\
                            "billingCycle":"MONTHLY","price":149.00,"currency":"PEN","sensorLimit":20},\
                            {"id":"0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002","code":"ANNUAL","name":"Plan anual",\
                            "billingCycle":"ANNUAL","price":1490.00,"currency":"PEN","sensorLimit":20}]""")))
    @GatewayHeaders
    @GetMapping("/plans")
    public List<PlanResponse> listPlans(@Parameter(hidden = true) @CurrentAdministratorId UUID administratorId) {
        return billingService.listPlans().stream().map(PlanResponse::from).toList();
    }

    @Operation(summary = "Choose a plan for a building",
            description = "US05. Creates the subscription of the building as PENDING_PAYMENT; an approved payment "
                    + "(POST /api/v1/payments) activates it. A building can have only one subscription that is "
                    + "pending payment or active; an active one past its expiry is closed as EXPIRED and the "
                    + "building may choose again. The building is not checked against the Building service yet."
                    + GATEWAY_NOTE)
    @ApiResponse(responseCode = "201", description = "The subscription was created; Location points to it",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SubscriptionResponse.class),
                    examples = @ExampleObject(name = "Pending payment", value = """
                            {"id":"9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d","buildingId":"3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b",\
                            "plan":{"id":"0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001","code":"MONTHLY","name":"Plan mensual",\
                            "billingCycle":"MONTHLY","price":149.00,"currency":"PEN","sensorLimit":20},\
                            "status":"PENDING_PAYMENT","startsAt":null,"expiresAt":null,\
                            "createdAt":"2026-11-21T15:00:00Z"}""")))
    @ApiResponse(responseCode = "400", description = "Invalid data; field errors are listed in 'errors'",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Missing fields", value = """
                            {"type":"about:blank","title":"Bad Request","status":400,\
                            "detail":"Invalid request content.","instance":"/api/v1/subscriptions",\
                            "errors":{"buildingId":"must not be null","planCode":"must not be blank"}}""")))
    @ApiResponse(responseCode = "404", description = "The plan does not exist",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Unknown plan", value = """
                            {"type":"about:blank","title":"Not Found","status":404,\
                            "detail":"The plan does not exist.","instance":"/api/v1/subscriptions"}""")))
    @ApiResponse(responseCode = "409", description = "The building already has a subscription pending payment or active",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Already subscribed", value = """
                            {"type":"about:blank","title":"Conflict","status":409,\
                            "detail":"The building already has a subscription that is pending payment or active.",\
                            "instance":"/api/v1/subscriptions"}""")))
    @GatewayHeaders
    @PostMapping
    public ResponseEntity<SubscriptionResponse> choose(
            @Parameter(hidden = true) @CurrentAdministratorId UUID administratorId,
            @Valid @RequestBody ChooseSubscriptionRequest request
    ) {
        Subscription subscription = billingService.chooseSubscription(administratorId, request.buildingId(),
                request.planCode());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(subscription.getId()).toUri();
        return ResponseEntity.created(location)
                .body(SubscriptionResponse.from(subscription, subscription.getStatus()));
    }

    @Operation(summary = "Read a subscription",
            description = "US07. The plan, status and expiry of a subscription of the signed-in administrator. "
                    + "A subscription of another administrator gets the same 404 as one that does not exist."
                    + GATEWAY_NOTE)
    @ApiResponse(responseCode = "200", description = "The subscription",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SubscriptionResponse.class),
                    examples = @ExampleObject(name = "Active", value = SUBSCRIPTION_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "The subscription does not exist or belongs to another administrator",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Not found", value = SUBSCRIPTION_NOT_FOUND_EXAMPLE)))
    @GatewayHeaders
    @GetMapping("/{subscriptionId}")
    public SubscriptionResponse get(
            @Parameter(hidden = true) @CurrentAdministratorId UUID administratorId,
            @Parameter(description = "Id of the subscription", example = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
            @PathVariable UUID subscriptionId
    ) {
        return SubscriptionResponse.from(billingService.getSubscription(administratorId, subscriptionId));
    }

    @Operation(summary = "List the payments of a subscription",
            description = "US07. Every payment attempt of the subscription, approved or rejected, newest first."
                    + GATEWAY_NOTE)
    @ApiResponse(responseCode = "200", description = "The payments",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class)),
                    examples = @ExampleObject(name = "One payment", value = "[" + PAYMENT_EXAMPLE + "]")))
    @ApiResponse(responseCode = "404", description = "The subscription does not exist or belongs to another administrator",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Not found", value = SUBSCRIPTION_NOT_FOUND_EXAMPLE)))
    @GatewayHeaders
    @GetMapping("/{subscriptionId}/payments")
    public List<PaymentResponse> listPayments(
            @Parameter(hidden = true) @CurrentAdministratorId UUID administratorId,
            @Parameter(description = "Id of the subscription", example = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d")
            @PathVariable UUID subscriptionId
    ) {
        return billingService.listPayments(administratorId, subscriptionId).stream().map(PaymentResponse::from).toList();
    }
}
