package com.rumi.billing.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.billing.MutableClock;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

/**
 * Test doubles of the acceptance scenarios: a clock they set by hand, so that expiry dates are exact, the
 * per-scenario HTTP client and the state shared by the step classes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class AcceptanceTestConfiguration {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(Instant.now());
    }

    @Bean
    @ScenarioScope
    public ApiClient apiClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        return new ApiClient(mockMvc, objectMapper);
    }

    @Bean
    @ScenarioScope
    public ScenarioState scenarioState() {
        return new ScenarioState();
    }
}
