package com.rumi.billing.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US05 and US07.
 */
public class SubscriptionSteps {

    private static final String SUBSCRIPTIONS = "/api/v1/subscriptions";

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioState state;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @When("{string} lists the subscription plans")
    public void listsThePlans(String user) throws Exception {
        api.get(SUBSCRIPTIONS + "/plans", state.headersOf(user));
    }

    @When("{string} chooses the plan {string} for the building {string}")
    public void choosesAPlan(String user, String planCode, String building) throws Exception {
        api.post(SUBSCRIPTIONS, Map.of("buildingId", state.building(building).toString(), "planCode", planCode),
                state.headersOf(user));
        if (api.response().getStatus() == 201) {
            state.subscriptionChosen(building, UUID.fromString(api.body().path("id").asText()));
        }
    }

    @Given("{string} chose the plan {string} for the building {string}")
    public void choseAPlan(String user, String planCode, String building) throws Exception {
        choosesAPlan(user, planCode, building);
        assertThat(api.response().getStatus()).as(api.responseText()).isEqualTo(201);
    }

    @When("{string} reads the subscription")
    public void readsTheSubscription(String user) throws Exception {
        api.get(SUBSCRIPTIONS + "/" + state.subscriptionId(), state.headersOf(user));
    }

    @When("{string} lists the payments of the subscription")
    public void listsThePayments(String user) throws Exception {
        api.get(SUBSCRIPTIONS + "/" + state.subscriptionId() + "/payments", state.headersOf(user));
    }

    @Then("the plans are {string} at {string} PEN and {string} at {string} PEN")
    public void thePlansAre(String firstCode, String firstPrice, String secondCode, String secondPrice) throws Exception {
        JsonNode plans = api.body();
        assertThat(plans).hasSize(2);
        assertThat(plans.get(0).path("code").asText()).isEqualTo(firstCode);
        assertThat(plans.get(0).path("price").decimalValue()).isEqualByComparingTo(firstPrice);
        assertThat(plans.get(0).path("currency").asText()).isEqualTo("PEN");
        assertThat(plans.get(1).path("code").asText()).isEqualTo(secondCode);
        assertThat(plans.get(1).path("price").decimalValue()).isEqualByComparingTo(secondPrice);
    }

    @And("the response is a subscription {string} to the plan {string}")
    public void theResponseIsASubscription(String status, String planCode) throws Exception {
        assertThat(api.body().path("id").asText()).isEqualTo(state.subscriptionId().toString());
        assertThat(api.body().path("status").asText()).isEqualTo(status);
        assertThat(api.body().path("plan").path("code").asText()).isEqualTo(planCode);
    }

    @And("the response has the location of the subscription")
    public void theResponseHasTheLocation() {
        assertThat(api.response().getHeader("Location")).endsWith(SUBSCRIPTIONS + "/" + state.subscriptionId());
    }

    @And("the subscription expires at {string}")
    public void theSubscriptionExpiresAt(String instant) throws Exception {
        assertThat(api.body().path("expiresAt").asText()).isEqualTo(instant);
    }

    @And("the payment history is {string}")
    public void thePaymentHistoryIs(String statuses) throws Exception {
        assertThat(api.body()).extracting(payment -> payment.path("status").asText())
                .containsExactly(statuses.split(", "));
    }

    @And("the first subscription of {string} is stored as {string}")
    public void theFirstSubscriptionIsStoredAs(String building, String status) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE id = ?", String.class,
                state.firstSubscriptionOf(building))).isEqualTo(status);
    }
}
