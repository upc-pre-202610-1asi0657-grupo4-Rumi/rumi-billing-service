package com.rumi.billing.domain.service;

/**
 * Anti-Corruption Layer port to the external payment gateway. The domain and the application only know this port;
 * today the only adapter is a simulated gateway, and a real one (Culqi, Stripe) would be a second adapter.
 */
public interface PaymentGatewayClient {

    GatewayResult charge(ChargeRequest request);
}
