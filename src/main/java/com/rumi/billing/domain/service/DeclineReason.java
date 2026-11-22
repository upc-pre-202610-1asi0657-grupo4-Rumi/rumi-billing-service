package com.rumi.billing.domain.service;

/**
 * Short reason given by the payment gateway when it declines a charge. Only this code is shown to the client.
 */
public enum DeclineReason {

    INSUFFICIENT_FUNDS,
    CARD_DECLINED
}
