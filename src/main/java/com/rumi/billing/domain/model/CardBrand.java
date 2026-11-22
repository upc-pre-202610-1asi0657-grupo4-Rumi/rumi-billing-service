package com.rumi.billing.domain.model;

/**
 * Brand of the card used in a payment. It is one of the two card facts that are kept, with the last four digits.
 */
public enum CardBrand {

    VISA,
    MASTERCARD,
    AMEX,
    UNKNOWN
}
