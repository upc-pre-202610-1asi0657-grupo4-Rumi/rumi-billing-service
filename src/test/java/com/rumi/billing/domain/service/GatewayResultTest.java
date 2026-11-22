package com.rumi.billing.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayResultTest {

    @Test
    void anApprovedResultHasAReferenceAndNoDeclineReason() {
        GatewayResult result = GatewayResult.approved("sim_1");

        assertThat(result.approved()).isTrue();
        assertThat(result.reference()).isEqualTo("sim_1");
        assertThat(result.declineReason()).isEmpty();
    }

    @Test
    void aDeclinedResultHasAReferenceAndADeclineReason() {
        GatewayResult result = GatewayResult.declined("sim_2", DeclineReason.INSUFFICIENT_FUNDS);

        assertThat(result.approved()).isFalse();
        assertThat(result.reference()).isEqualTo("sim_2");
        assertThat(result.declineReason()).contains(DeclineReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void requiresAReferenceAndADeclineReasonWhenDeclined() {
        assertThatThrownBy(() -> GatewayResult.approved(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GatewayResult.declined("sim_3", null)).isInstanceOf(NullPointerException.class);
    }
}
