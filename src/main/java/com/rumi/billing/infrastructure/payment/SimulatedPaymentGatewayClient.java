package com.rumi.billing.infrastructure.payment;

import com.rumi.billing.domain.service.ChargeRequest;
import com.rumi.billing.domain.service.DeclineReason;
import com.rumi.billing.domain.service.GatewayResult;
import com.rumi.billing.domain.service.PaymentGatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated adapter of the {@link PaymentGatewayClient} port, used until a real gateway (Culqi, Stripe) is
 * contracted. Its test cards: {@value #DECLINED_CARD} is declined for insufficient funds and any other card that
 * passed validation is approved. Nothing is charged and the card never leaves the process; only the brand and the
 * last four digits are logged.
 */
@Component
public class SimulatedPaymentGatewayClient implements PaymentGatewayClient {

    public static final String DECLINED_CARD = "4000000000000002";
    static final String REFERENCE_PREFIX = "sim_";

    private static final Logger LOG = LoggerFactory.getLogger(SimulatedPaymentGatewayClient.class);

    @Override
    public GatewayResult charge(ChargeRequest request) {
        String reference = REFERENCE_PREFIX + UUID.randomUUID();
        GatewayResult result = DECLINED_CARD.equals(request.card().number())
                ? GatewayResult.declined(reference, DeclineReason.INSUFFICIENT_FUNDS)
                : GatewayResult.approved(reference);
        LOG.info("Simulated charge {} of {} {} with {}: {}", reference, request.amount().amount(),
                request.amount().currency(), request.card(), result.approved() ? "approved" : "declined");
        return result;
    }
}
