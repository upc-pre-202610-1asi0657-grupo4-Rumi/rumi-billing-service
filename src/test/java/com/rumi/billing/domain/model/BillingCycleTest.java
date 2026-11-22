package com.rumi.billing.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillingCycleTest {

    @ParameterizedTest
    @CsvSource({
            "2026-03-15T10:30:00Z, 2026-04-15T10:30:00Z",
            "2026-12-20T00:00:00Z, 2027-01-20T00:00:00Z",
            "2026-01-31T08:00:00Z, 2026-02-28T08:00:00Z",
            "2028-01-31T08:00:00Z, 2028-02-29T08:00:00Z",
            "2026-05-31T23:59:59Z, 2026-06-30T23:59:59Z"
    })
    void aMonthlyCycleAddsOneCalendarMonthInUtc(Instant from, Instant expected) {
        assertThat(BillingCycle.MONTHLY.nextExpiry(from)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-03-15T10:30:00Z, 2027-03-15T10:30:00Z",
            "2028-02-29T12:00:00Z, 2029-02-28T12:00:00Z"
    })
    void anAnnualCycleAddsOneCalendarYearInUtc(Instant from, Instant expected) {
        assertThat(BillingCycle.ANNUAL.nextExpiry(from)).isEqualTo(expected);
    }

    @Test
    void requiresAStartingInstant() {
        assertThatThrownBy(() -> BillingCycle.MONTHLY.nextExpiry(null)).isInstanceOf(NullPointerException.class);
    }
}
