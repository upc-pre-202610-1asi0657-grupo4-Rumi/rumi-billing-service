package com.rumi.billing.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps of US24. The simulated gateway declines 4000000000000002 and approves any other valid card.
 */
public class PaymentSteps {

    private static final String PAYMENTS = "/api/v1/payments";
    private static final String HOLDER = "Ana Torres";
    private static final String CVV = "123";

    @Autowired
    private ApiClient api;

    @Autowired
    private ScenarioState state;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @When("{string} pays the subscription with the card {string}")
    public void paysTheSubscription(String user, String number) throws Exception {
        pay(user, number, null);
    }

    @When("{string} pays the subscription with the card {string} and the idempotency key {string}")
    public void paysWithAnIdempotencyKey(String user, String number, String key) throws Exception {
        pay(user, number, key);
    }

    @Given("{string} paid the subscription with the card {string}")
    public void paidTheSubscription(String user, String number) throws Exception {
        pay(user, number, null);
        assertThat(api.response().getStatus()).as(api.responseText()).isEqualTo(201);
    }

    @Given("{string} tried to pay the subscription with the card {string}")
    public void triedToPayTheSubscription(String user, String number) throws Exception {
        pay(user, number, null);
        assertThat(api.response().getStatus()).as(api.responseText()).isEqualTo(402);
    }

    @Then("the payment is {string} with a {string} card ending in {string}")
    public void thePaymentIs(String status, String brand, String last4) throws Exception {
        assertThat(api.body().path("subscriptionId").asText()).isEqualTo(state.subscriptionId().toString());
        assertThat(api.body().path("status").asText()).isEqualTo(status);
        assertThat(api.body().path("cardBrand").asText()).isEqualTo(brand);
        assertThat(api.body().path("cardLast4").asText()).isEqualTo(last4);
        assertThat(api.body().path("amount").decimalValue()).isEqualByComparingTo("149.00");
    }

    @And("the response does not contain the card")
    public void theResponseDoesNotContainTheCard() throws Exception {
        assertThat(api.responseText())
                .doesNotContain(state.lastCardNumber())
                .doesNotContain(HOLDER)
                .doesNotContain("\"cvv\"");
    }

    @And("the decline reason is {string}")
    public void theDeclineReasonIs(String reason) throws Exception {
        assertThat(api.body().path("declineReason").asText()).isEqualTo(reason);
        assertThat(api.body().path("paymentId").asText()).isNotBlank();
    }

    @And("the subscription is {string}")
    public void theSubscriptionIs(String status) throws Exception {
        api.get("/api/v1/subscriptions/" + state.subscriptionId(), adminHeaders());
        assertThat(api.body().path("status").asText()).isEqualTo(status);
    }

    @And("the subscription is {string} until {string}")
    public void theSubscriptionIsUntil(String status, String expiresAt) throws Exception {
        theSubscriptionIs(status);
        assertThat(api.body().path("expiresAt").asText()).isEqualTo(expiresAt);
    }

    @And("the subscription has {int} payment(s)")
    public void theSubscriptionHasPayments(int count) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM payments WHERE subscription_id = ?",
                Integer.class, state.subscriptionId())).isEqualTo(count);
    }

    private void pay(String user, String number, String idempotencyKey) throws Exception {
        state.cardSent(number);
        Map<String, String> headers = new HashMap<>(state.headersOf(user));
        if (idempotencyKey != null) {
            headers.put("Idempotency-Key", idempotencyKey);
        }
        api.post(PAYMENTS, Map.of(
                "subscriptionId", state.subscriptionId().toString(),
                "card", Map.of("number", number, "holderName", HOLDER, "expiryMonth", 12, "expiryYear", 2030,
                        "cvv", CVV)), headers);
    }

    private Map<String, String> adminHeaders() {
        return state.headersOf("Ana");
    }
}
