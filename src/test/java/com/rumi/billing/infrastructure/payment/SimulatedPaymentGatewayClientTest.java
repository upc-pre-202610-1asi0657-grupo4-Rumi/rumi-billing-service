package com.rumi.billing.infrastructure.payment;

import com.rumi.billing.domain.model.CardDetails;
import com.rumi.billing.domain.model.Money;
import com.rumi.billing.domain.service.ChargeRequest;
import com.rumi.billing.domain.service.DeclineReason;
import com.rumi.billing.domain.service.GatewayResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class SimulatedPaymentGatewayClientTest {

    private static final Instant NOW = Instant.parse("2026-11-21T15:00:00Z");
    private static final Money PRICE = new Money(new BigDecimal("149.00"), "PEN");

    private final SimulatedPaymentGatewayClient gateway = new SimulatedPaymentGatewayClient();

    private static ChargeRequest charge(String number, String cvv) {
        return new ChargeRequest(PRICE, CardDetails.of(number, "Ana Torres", 12, 2028, cvv, NOW));
    }

    @Test
    void declinesTheTestCardForInsufficientFunds() {
        GatewayResult result = gateway.charge(charge(SimulatedPaymentGatewayClient.DECLINED_CARD, "123"));

        assertThat(result.approved()).isFalse();
        assertThat(result.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4111111111111111", "5500000000000004", "6011111111111117"})
    void approvesAnyOtherValidCard(String number) {
        GatewayResult result = gateway.charge(charge(number, "123"));

        assertThat(result.approved()).isTrue();
        assertThat(result.declineReason()).isEmpty();
    }

    @Test
    void givesEveryChargeItsOwnSimulatedReference() {
        String first = gateway.charge(charge("4111111111111111", "123")).reference();
        String second = gateway.charge(charge(SimulatedPaymentGatewayClient.DECLINED_CARD, "123")).reference();

        assertThat(first).startsWith("sim_").hasSizeLessThanOrEqualTo(64).isNotEqualTo(second);
        assertThat(UUID.fromString(first.substring("sim_".length()))).isNotNull();
        assertThat(second).startsWith("sim_");
    }

    @Test
    void neverLogsTheCardNumberTheCvvOrTheHolder(CapturedOutput output) {
        gateway.charge(charge("4111111111111111", "987"));
        gateway.charge(charge(SimulatedPaymentGatewayClient.DECLINED_CARD, "987"));

        assertThat(output.getAll())
                .contains("sim_")
                .doesNotContain("4111111111111111")
                .doesNotContain(SimulatedPaymentGatewayClient.DECLINED_CARD)
                .doesNotContain("987")
                .doesNotContain("Ana Torres");
    }
}
