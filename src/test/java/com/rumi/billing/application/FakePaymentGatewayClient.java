package com.rumi.billing.application;

import com.rumi.billing.domain.service.ChargeRequest;
import com.rumi.billing.domain.service.DeclineReason;
import com.rumi.billing.domain.service.GatewayResult;
import com.rumi.billing.domain.service.PaymentGatewayClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Approves every charge unless told to decline, and keeps the charges it received.
 */
public class FakePaymentGatewayClient implements PaymentGatewayClient {

    private final List<ChargeRequest> charges = new ArrayList<>();
    private boolean decline;

    public void declineCharges() {
        decline = true;
    }

    @Override
    public GatewayResult charge(ChargeRequest request) {
        charges.add(request);
        String reference = "fake_" + charges.size();
        return decline ? GatewayResult.declined(reference, DeclineReason.INSUFFICIENT_FUNDS)
                : GatewayResult.approved(reference);
    }

    public List<ChargeRequest> charges() {
        return charges;
    }
}
