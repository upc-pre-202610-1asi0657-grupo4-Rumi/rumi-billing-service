package com.rumi.billing.domain.model;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * The card of a payment, valid at the time of the payment: the number passes the Luhn check, the card has not
 * expired and the CVV has the length of its brand. It only lives while the payment gateway is called and is never
 * stored; {@link #toString()} shows the brand and the last four digits only, so that it cannot leak into logs.
 * Validation messages never repeat the rejected values.
 */
public final class CardDetails {

    public static final int MAX_HOLDER_NAME_LENGTH = 100;

    private final String number;
    private final String holderName;
    private final int expiryMonth;
    private final int expiryYear;
    private final String cvv;
    private final CardBrand brand;

    private CardDetails(String number, String holderName, int expiryMonth, int expiryYear, String cvv) {
        this.number = number;
        this.holderName = holderName;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cvv = cvv;
        this.brand = brandOf(number);
    }

    public static CardDetails of(String number, String holderName, int expiryMonth, int expiryYear, String cvv,
                                 Instant now) {
        Objects.requireNonNull(now, "The time of the payment is required");
        String digits = requireValidNumber(number);
        if (expiryMonth < 1 || expiryMonth > 12) {
            throw new IllegalArgumentException("Card expiry month must be between 1 and 12");
        }
        if (YearMonth.of(expiryYear, expiryMonth).isBefore(YearMonth.from(now.atZone(ZoneOffset.UTC)))) {
            throw new IllegalArgumentException("Card is expired");
        }
        int cvvLength = brandOf(digits) == CardBrand.AMEX ? 4 : 3;
        if (cvv == null || !cvv.matches("\\d{" + cvvLength + "}")) {
            throw new IllegalArgumentException("Card CVV is not valid");
        }
        if (holderName == null || holderName.isBlank()) {
            throw new IllegalArgumentException("Card holder name is required");
        }
        if (holderName.length() > MAX_HOLDER_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Card holder name must have at most " + MAX_HOLDER_NAME_LENGTH + " characters");
        }
        return new CardDetails(digits, holderName.trim(), expiryMonth, expiryYear, cvv);
    }

    public String number() {
        return number;
    }

    public String holderName() {
        return holderName;
    }

    public int expiryMonth() {
        return expiryMonth;
    }

    public int expiryYear() {
        return expiryYear;
    }

    public String cvv() {
        return cvv;
    }

    public CardBrand brand() {
        return brand;
    }

    public String last4() {
        return number.substring(number.length() - 4);
    }

    @Override
    public String toString() {
        return "CardDetails[brand=" + brand + ", last4=" + last4() + "]";
    }

    private static String requireValidNumber(String number) {
        String digits = number == null ? "" : number.replace(" ", "");
        if (!digits.matches("\\d{12,19}") || !passesLuhn(digits)) {
            throw new IllegalArgumentException("Card number is not valid");
        }
        return digits;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private static CardBrand brandOf(String digits) {
        if (digits.startsWith("4")) {
            return CardBrand.VISA;
        }
        int firstTwo = Integer.parseInt(digits.substring(0, 2));
        if (firstTwo >= 51 && firstTwo <= 55) {
            return CardBrand.MASTERCARD;
        }
        if (firstTwo == 34 || firstTwo == 37) {
            return CardBrand.AMEX;
        }
        return CardBrand.UNKNOWN;
    }
}
