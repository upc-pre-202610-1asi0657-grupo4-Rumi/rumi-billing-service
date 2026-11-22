package com.rumi.billing.infrastructure.web;

import com.rumi.billing.application.BillingApplicationService;
import com.rumi.billing.application.PaymentReceipt;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.infrastructure.web.dto.PaymentRequest;
import com.rumi.billing.infrastructure.web.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.rumi.billing.infrastructure.web.ApiDocumentation.FORBIDDEN_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.GATEWAY_NOTE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.PAYMENT_EXAMPLE;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.UNAUTHORIZED_EXAMPLE;

/**
 * Online payment of a subscription of the signed-in administrator (US24).
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Online payment of a subscription (US24)")
public class PaymentController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    static final int MAX_IDEMPOTENCY_KEY_LENGTH = Payment.MAX_IDEMPOTENCY_KEY_LENGTH;
    static final String IDEMPOTENCY_KEY_MESSAGE =
            "The Idempotency-Key header must have between 1 and " + MAX_IDEMPOTENCY_KEY_LENGTH + " characters.";

    private final BillingApplicationService billingService;

    public PaymentController(BillingApplicationService billingService) {
        this.billingService = billingService;
    }

    @Operation(summary = "Pay a subscription online",
            description = "US24. Charges the price of the plan to the card through the payment gateway (simulated "
                    + "today: 4000000000000002 is declined, any other valid card is approved). An approved payment "
                    + "activates a subscription pending payment, renews an active one from its current expiry, or "
                    + "restarts an expired one from now. A declined payment is kept as REJECTED and answered with "
                    + "402. Only the brand and the last four digits of the card are kept. Sending the same "
                    + "Idempotency-Key again answers with the payment already made (200) and charges nothing."
                    + GATEWAY_NOTE)
    @Parameter(name = IDEMPOTENCY_KEY_HEADER, description = "Optional key, at most 64 characters, that makes "
            + "retries safe: the same key never charges twice", example = "order-2026-11-21-0001")
    @ApiResponse(responseCode = "201", description = "The payment was approved",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaymentResponse.class),
                    examples = @ExampleObject(name = "Approved", value = PAYMENT_EXAMPLE)))
    @ApiResponse(responseCode = "200", description = "The Idempotency-Key was already used for this subscription; "
            + "nothing was charged and the stored payment is returned",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaymentResponse.class),
                    examples = @ExampleObject(name = "Repeated key", value = PAYMENT_EXAMPLE)))
    @ApiResponse(responseCode = "400", description = "Invalid data: missing fields, a number that fails the Luhn "
            + "check, an expired card or a CVV of the wrong length. The card data is never echoed",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Invalid number", value = """
                            {"type":"about:blank","title":"Bad Request","status":400,\
                            "detail":"Card number is not valid","instance":"/api/v1/payments"}""")))
    @ApiResponse(responseCode = "401", description = "The request did not come through the gateway with a valid token",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Missing X-User-Id", value = UNAUTHORIZED_EXAMPLE)))
    @ApiResponse(responseCode = "402", description = "The gateway declined the payment; the attempt is kept as REJECTED",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Declined", value = """
                            {"type":"about:blank","title":"Payment Required","status":402,\
                            "detail":"The payment was declined.","instance":"/api/v1/payments",\
                            "paymentId":"1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f","declineReason":"INSUFFICIENT_FUNDS"}""")))
    @ApiResponse(responseCode = "403", description = "The signed-in user is not an administrator",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Resident", value = FORBIDDEN_EXAMPLE)))
    @ApiResponse(responseCode = "404", description = "The subscription does not exist or belongs to another administrator",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Not found", value = """
                            {"type":"about:blank","title":"Not Found","status":404,\
                            "detail":"The subscription does not exist.","instance":"/api/v1/payments"}""")))
    @ApiResponse(responseCode = "409", description = "The subscription is cancelled or replaced, or the "
            + "Idempotency-Key was used for another subscription",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(name = "Key of another subscription", value = """
                            {"type":"about:blank","title":"Conflict","status":409,\
                            "detail":"The Idempotency-Key was already used for another subscription.",\
                            "instance":"/api/v1/payments"}""")))
    @GatewayHeaders
    @PostMapping
    public ResponseEntity<PaymentResponse> pay(
            @Parameter(hidden = true) @CurrentAdministratorId UUID administratorId,
            @Parameter(hidden = true) @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request
    ) {
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH)) {
            throw new IllegalArgumentException(IDEMPOTENCY_KEY_MESSAGE);
        }
        PaymentReceipt receipt = billingService.pay(administratorId, request.subscriptionId(),
                request.card().toCardInput(), idempotencyKey);
        HttpStatus status = receipt.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(PaymentResponse.from(receipt.payment()));
    }
}
