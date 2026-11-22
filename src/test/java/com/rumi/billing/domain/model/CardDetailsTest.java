package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardDetailsTest {

    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");
    private static final String VISA = "4111111111111111";

    private static CardDetails card(String number, int month, int year, String cvv) {
        return CardDetails.of(number, "Ana Torres", month, year, cvv, NOW);
    }

    @Test
    void keepsAValidCard() {
        CardDetails card = card(VISA, 12, 2028, "123");

        assertThat(card.number()).isEqualTo(VISA);
        assertThat(card.holderName()).isEqualTo("Ana Torres");
        assertThat(card.brand()).isEqualTo(CardBrand.VISA);
        assertThat(card.last4()).isEqualTo("1111");
    }

    @Test
    void ignoresSpacesBetweenTheDigits() {
        assertThat(card("4111 1111 1111 1111", 12, 2028, "123").number()).isEqualTo(VISA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111112", "1234567890123456", "4111", "41111111111111111111", "4111-1111-1111-1111x", ""})
    void rejectsANumberThatFailsTheLuhnCheckOrHasAWrongLength(String number) {
        assertThatThrownBy(() -> card(number, 12, 2028, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card number is not valid");
    }

    @ParameterizedTest
    @CsvSource({
            "4111111111111111, VISA",
            "5500000000000004, MASTERCARD",
            "5105105105105100, MASTERCARD",
            "340000000000009, AMEX",
            "378282246310005, AMEX",
            "6011111111111117, UNKNOWN"
    })
    void derivesTheBrandFromThePrefix(String number, CardBrand brand) {
        String cvv = brand == CardBrand.AMEX ? "1234" : "123";
        assertThat(card(number, 12, 2028, cvv).brand()).isEqualTo(brand);
    }

    @Test
    void aCardIsValidUntilTheEndOfItsExpiryMonth() {
        assertThat(card(VISA, 11, 2026, "123").last4()).isEqualTo("1111");
        assertThatThrownBy(() -> card(VISA, 10, 2026, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card is expired");
        assertThatThrownBy(() -> card(VISA, 12, 2025, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card is expired");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13})
    void rejectsAnExpiryMonthOutOfRange(int month) {
        assertThatThrownBy(() -> card(VISA, month, 2028, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card expiry month must be between 1 and 12");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "1234", "12a", ""})
    void rejectsACvvThatIsNotThreeDigitsForVisa(String cvv) {
        assertThatThrownBy(() -> card(VISA, 12, 2028, cvv))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card CVV is not valid");
    }

    @Test
    void anAmexCardNeedsAFourDigitCvv() {
        assertThat(card("378282246310005", 12, 2028, "1234").brand()).isEqualTo(CardBrand.AMEX);
        assertThatThrownBy(() -> card("378282246310005", 12, 2028, "123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card CVV is not valid");
    }

    @Test
    void requiresAHolderName() {
        assertThatThrownBy(() -> CardDetails.of(VISA, " ", 12, 2028, "123", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card holder name is required");
    }

    @Test
    void toStringHidesTheNumberTheCvvAndTheHolder() {
        CardDetails card = CardDetails.of(VISA, "Ana Torres", 12, 2028, "987", NOW);

        assertThat(card.toString())
                .doesNotContain(VISA)
                .doesNotContain("987")
                .doesNotContain("Ana Torres")
                .contains("VISA")
                .contains("1111");
    }
}
