package com.rumi.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rumi.billing.infrastructure.payment.SimulatedPaymentGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The whole payment flow on PostgreSQL: a declined attempt is kept and listed, an approved one activates the
 * subscription, and the card number, the CVV and the holder name end up in no response, no log line and no
 * text column of any table.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainer.class)
@ExtendWith(OutputCaptureExtension.class)
class PaymentFlowIntegrationTest {

    private static final String APPROVED_CARD = "5500000000000004";
    private static final String HOLDER = "Zoila Quispe Mamani";
    private static final String CVV = "731";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID ownerId = UUID.randomUUID();
    private final List<String> responses = new ArrayList<>();

    @Test
    void aRejectedAttemptIsStoredAndListedAndNoCardDataIsKept(CapturedOutput output) throws Exception {
        String subscriptionId = send(post("/api/v1/subscriptions").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "buildingId", UUID.randomUUID().toString(), "planCode", "MONTHLY"))), 201)
                .path("id").asText();

        JsonNode declined = send(pay(subscriptionId, SimulatedPaymentGatewayClient.DECLINED_CARD), 402);
        assertThat(declined.path("declineReason").asText()).isEqualTo("INSUFFICIENT_FUNDS");

        JsonNode payments = send(get("/api/v1/subscriptions/" + subscriptionId + "/payments"), 200);
        assertThat(payments).singleElement().satisfies(payment -> {
            assertThat(payment.path("id").asText()).isEqualTo(declined.path("paymentId").asText());
            assertThat(payment.path("status").asText()).isEqualTo("REJECTED");
            assertThat(payment.path("cardLast4").asText()).isEqualTo("0002");
        });
        assertThat(send(get("/api/v1/subscriptions/" + subscriptionId), 200).path("status").asText())
                .isEqualTo("PENDING_PAYMENT");

        JsonNode approved = send(pay(subscriptionId, APPROVED_CARD), 201);
        assertThat(approved.path("cardBrand").asText()).isEqualTo("MASTERCARD");
        assertThat(send(get("/api/v1/subscriptions/" + subscriptionId), 200).path("status").asText())
                .isEqualTo("ACTIVE");
        assertThat(send(get("/api/v1/subscriptions/" + subscriptionId + "/payments"), 200))
                .extracting(payment -> payment.path("status").asText()).containsExactly("APPROVED", "REJECTED");

        for (String card : List.of(APPROVED_CARD, SimulatedPaymentGatewayClient.DECLINED_CARD)) {
            assertThat(responses).noneMatch(body -> body.contains(card));
            assertThat(output.getAll()).doesNotContain(card);
            assertThat(textColumnsContaining(card)).isEmpty();
        }
        assertThat(responses).noneMatch(body -> body.contains(HOLDER) || body.contains("\"cvv\""));
        assertThat(output.getAll()).doesNotContain(HOLDER);
        assertThat(textColumnsContaining(HOLDER)).isEmpty();
        // Every text value stored is known, so the CVV can be looked up exactly and not as a substring.
        assertThat(textColumnsEqualTo(CVV)).isEmpty();
    }

    private MockHttpServletRequestBuilder pay(String subscriptionId, String number) throws Exception {
        return post("/api/v1/payments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "subscriptionId", subscriptionId,
                        "card", Map.of("number", number, "holderName", HOLDER, "expiryMonth", 12,
                                "expiryYear", 2099, "cvv", CVV))));
    }

    private JsonNode send(MockHttpServletRequestBuilder request, int expectedStatus) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(request
                        .header("X-User-Id", ownerId.toString()).header("X-Role", "ADMINISTRATOR"))
                .andReturn().getResponse();
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        responses.add(body);
        assertThat(response.getStatus()).as(body).isEqualTo(expectedStatus);
        return objectMapper.readTree(body);
    }

    private List<String> textColumnsContaining(String value) {
        return textColumnsMatching("LIKE ?", "%" + value + "%");
    }

    private List<String> textColumnsEqualTo(String value) {
        return textColumnsMatching("= ?", value);
    }

    private List<String> textColumnsMatching(String condition, String argument) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
                SELECT table_name, column_name FROM information_schema.columns
                WHERE table_schema = 'public' AND data_type IN ('character varying', 'character', 'text')""");
        assertThat(columns).isNotEmpty();
        List<String> found = new ArrayList<>();
        for (Map<String, Object> column : columns) {
            String table = (String) column.get("table_name");
            String name = (String) column.get("column_name");
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE " + name + " " + condition, Integer.class, argument);
            if (count != null && count > 0) {
                found.add(table + "." + name);
            }
        }
        return found;
    }
}
