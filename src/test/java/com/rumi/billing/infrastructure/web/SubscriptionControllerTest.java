package com.rumi.billing.infrastructure.web;

import com.rumi.billing.application.BillingApplicationService;
import com.rumi.billing.application.BuildingAlreadySubscribedException;
import com.rumi.billing.application.PlanNotFoundException;
import com.rumi.billing.application.SubscriptionDetails;
import com.rumi.billing.application.SubscriptionNotFoundException;
import com.rumi.billing.domain.model.BillingFixtures;
import com.rumi.billing.domain.model.Payment;
import com.rumi.billing.domain.model.PaymentStatus;
import com.rumi.billing.domain.model.SubscriptionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static com.rumi.billing.infrastructure.web.WebFixtures.BUILDING_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.OWNER_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.SUBSCRIPTION_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.asAdministrator;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SubscriptionController.class, HealthController.class})
class SubscriptionControllerTest {

    private static final String SUBSCRIPTIONS = "/api/v1/subscriptions";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingApplicationService billingService;

    @Test
    void listsThePlans() throws Exception {
        when(billingService.listPlans()).thenReturn(List.of(BillingFixtures.MONTHLY, BillingFixtures.ANNUAL));

        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/plans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001"))
                .andExpect(jsonPath("$[0].code").value("MONTHLY"))
                .andExpect(jsonPath("$[0].name").value("Plan mensual"))
                .andExpect(jsonPath("$[0].billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$[0].price").value(149.00))
                .andExpect(jsonPath("$[0].currency").value("PEN"))
                .andExpect(jsonPath("$[0].sensorLimit").value(20))
                .andExpect(jsonPath("$[1].code").value("ANNUAL"));
    }

    @Test
    void theHealthEndpointStillAnswersWithoutHeaders() throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS + "/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        verifyNoInteractions(billingService);
    }

    @Test
    void choosesAPlanAndAnswersCreatedWithTheLocation() throws Exception {
        when(billingService.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY")).thenReturn(WebFixtures.pending());

        choose(chooseJson(BUILDING_ID.toString(), "MONTHLY"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/subscriptions/" + SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID.toString()))
                .andExpect(jsonPath("$.buildingId").value(BUILDING_ID.toString()))
                .andExpect(jsonPath("$.plan.code").value("MONTHLY"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.startsAt").isEmpty())
                .andExpect(jsonPath("$.expiresAt").isEmpty())
                .andExpect(jsonPath("$.createdAt").value("2026-11-21T15:00:00Z"));
    }

    @Test
    void rejectsAChoiceWithMissingFields() throws Exception {
        choose("{}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.buildingId").value("must not be null"))
                .andExpect(jsonPath("$.errors.planCode").value("must not be blank"));
        verifyNoInteractions(billingService);
    }

    @Test
    void rejectsABuildingIdThatIsNotAUuid() throws Exception {
        choose(chooseJson("building-1", "MONTHLY")).andExpect(status().isBadRequest());
        verifyNoInteractions(billingService);
    }

    @Test
    void anUnknownPlanIsNotFound() throws Exception {
        when(billingService.chooseSubscription(OWNER_ID, BUILDING_ID, "WEEKLY")).thenThrow(new PlanNotFoundException());

        choose(chooseJson(BUILDING_ID.toString(), "WEEKLY"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(PlanNotFoundException.MESSAGE));
    }

    @Test
    void aBuildingWithAnOpenSubscriptionConflicts() throws Exception {
        when(billingService.chooseSubscription(OWNER_ID, BUILDING_ID, "MONTHLY"))
                .thenThrow(new BuildingAlreadySubscribedException());

        choose(chooseJson(BUILDING_ID.toString(), "MONTHLY"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(BuildingAlreadySubscribedException.MESSAGE));
    }

    @Test
    void readsASubscriptionWithItsPlanAndExpiry() throws Exception {
        when(billingService.getSubscription(OWNER_ID, SUBSCRIPTION_ID))
                .thenReturn(new SubscriptionDetails(WebFixtures.active(), SubscriptionStatus.ACTIVE));

        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID.toString()))
                .andExpect(jsonPath("$.plan.name").value("Plan mensual"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.startsAt").value("2026-11-21T15:05:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-21T15:05:00Z"));
    }

    @Test
    void reportsTheStatusAsOfNow() throws Exception {
        when(billingService.getSubscription(OWNER_ID, SUBSCRIPTION_ID))
                .thenReturn(new SubscriptionDetails(WebFixtures.active(), SubscriptionStatus.EXPIRED));

        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID)))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void aSubscriptionThatDoesNotExistOrBelongsToAnotherOwnerIsNotFound() throws Exception {
        when(billingService.getSubscription(OWNER_ID, SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException());
        when(billingService.listPayments(OWNER_ID, SUBSCRIPTION_ID)).thenThrow(new SubscriptionNotFoundException());

        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(SubscriptionNotFoundException.MESSAGE));
        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID + "/payments")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aSubscriptionIdThatIsNotAUuidIsABadRequest() throws Exception {
        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        verifyNoInteractions(billingService);
    }

    @Test
    void listsThePaymentsIncludingRejectedAttempts() throws Exception {
        Payment approved = WebFixtures.payment(PaymentStatus.APPROVED);
        Payment rejected = WebFixtures.payment(PaymentStatus.REJECTED);
        when(billingService.listPayments(OWNER_ID, SUBSCRIPTION_ID)).thenReturn(List.of(approved, rejected));

        mockMvc.perform(asAdministrator(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID + "/payments")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(WebFixtures.PAYMENT_ID.toString()))
                .andExpect(jsonPath("$[0].subscriptionId").value(SUBSCRIPTION_ID.toString()))
                .andExpect(jsonPath("$[0].amount").value(149.00))
                .andExpect(jsonPath("$[0].currency").value("PEN"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].cardBrand").value("VISA"))
                .andExpect(jsonPath("$[0].cardLast4").value("1111"))
                .andExpect(jsonPath("$[0].paidAt").value("2026-11-21T15:05:00Z"))
                .andExpect(jsonPath("$[0].gatewayReference").doesNotExist())
                .andExpect(jsonPath("$[1].status").value("REJECTED"));
    }

    @Test
    void answersUnauthorizedWithoutTheGatewayUserHeader() throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS + "/plans").header("X-Role", "ADMINISTRATOR"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value(MissingUserIdException.MESSAGE))
                .andExpect(jsonPath("$.instance").value(SUBSCRIPTIONS + "/plans"));
        verifyNoInteractions(billingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-uuid", "1-2-3-4-5"})
    void answersUnauthorizedWhenTheUserHeaderIsNotACanonicalUuid(String userId) throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS + "/plans").header("X-User-Id", userId).header("X-Role", "ADMINISTRATOR"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(billingService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESIDENT", "administrator", ""})
    void answersForbiddenToAnyoneButAnAdministrator(String role) throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS + "/plans").header("X-User-Id", OWNER_ID.toString()).header("X-Role", role))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value(NotAdministratorException.MESSAGE));
        verifyNoInteractions(billingService);
    }

    @Test
    void answersForbiddenWithoutTheRoleHeader() throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS + "/" + SUBSCRIPTION_ID).header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(billingService);
    }

    @Test
    void checksTheGatewayHeadersBeforeTheBody() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS).header("X-User-Id", OWNER_ID.toString()).header("X-Role", "RESIDENT")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    private ResultActions choose(String body) throws Exception {
        return mockMvc.perform(asAdministrator(post(SUBSCRIPTIONS)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String chooseJson(String buildingId, String planCode) {
        return """
                {"buildingId":"%s","planCode":"%s"}""".formatted(buildingId, planCode);
    }
}
