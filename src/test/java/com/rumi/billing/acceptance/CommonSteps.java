package com.rumi.billing.acceptance;

import com.rumi.billing.MutableClock;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps shared by every feature. The gateway is played as rumi-api-gateway does it: a signed-in user is a pair of
 * X-User-Id and X-Role headers.
 */
public class CommonSteps {

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioState state;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    /**
     * Every scenario starts with no subscriptions or payments; the seeded plans stay.
     */
    @Before
    public void startWithNoSubscriptions() {
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM subscriptions");
        clock.setInstant(Instant.now());
    }

    @Given("the administrator {string} is signed in through the gateway")
    public void anAdministratorIsSignedIn(String name) {
        state.signIn(name, "ADMINISTRATOR");
    }

    @Given("the resident {string} is signed in through the gateway")
    public void aResidentIsSignedIn(String name) {
        state.signIn(name, "RESIDENT");
    }

    @Given("the time is {string}")
    public void theTimeIs(String instant) {
        clock.setInstant(Instant.parse(instant));
    }

    @Given("{int} days have passed")
    public void daysHavePassed(int days) {
        clock.advance(Duration.ofDays(days));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) throws Exception {
        assertThat(api.response().getStatus()).as(api.responseText()).isEqualTo(status);
    }

    @And("the response is a problem detail with the message {string}")
    public void theResponseIsAProblemDetail(String message) throws Exception {
        assertThat(MediaType.parseMediaType(api.response().getContentType()))
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(api.body().path("status").asInt()).isEqualTo(api.response().getStatus());
        assertThat(api.body().path("detail").asText()).isEqualTo(message);
    }
}
