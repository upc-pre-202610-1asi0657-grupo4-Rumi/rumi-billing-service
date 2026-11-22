package com.rumi.billing.infrastructure.web;

import com.rumi.billing.application.BillingApplicationService;
import com.rumi.billing.application.CardInput;
import com.rumi.billing.application.IdempotencyKeyConflictException;
import com.rumi.billing.application.PaymentReceipt;
import com.rumi.billing.application.PaymentRejectedException;
import com.rumi.billing.application.SubscriptionNotFoundException;
import com.rumi.billing.application.SubscriptionNotPayableException;
import com.rumi.billing.domain.model.PaymentStatus;
import com.rumi.billing.domain.service.DeclineReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static com.rumi.billing.infrastructure.web.WebFixtures.OWNER_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.PAYMENT_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.SUBSCRIPTION_ID;
import static com.rumi.billing.infrastructure.web.WebFixtures.asAdministrator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    private static final String PAYMENTS = "/api/v1/payments";
    private static final String NUMBER = "4111111111111111";
    private static final String HOLDER = "Ana Torres";
    private static final CardInput CARD = new CardInput(NUMBER, HOLDER, 12, 2030, "123");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillingApplicationService billingService;

    @Test
    void anApprovedPaymentAnswersCreatedWithoutTheCard() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, null))
                .thenReturn(new PaymentReceipt(WebFixtures.payment(PaymentStatus.APPROVED), true));

        pay(json(NUMBER, "123"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.subscriptionId").value(SUBSCRIPTION_ID.toString()))
                .andExpect(jsonPath("$.amount").value(149.00))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.cardBrand").value("VISA"))
                .andExpect(jsonPath("$.cardLast4").value("1111"))
                .andExpect(jsonPath("$.paidAt").value("2026-11-21T15:05:00Z"))
                .andExpect(jsonPath("$.card").doesNotExist())
                .andExpect(jsonPath("$.cvv").doesNotExist())
                .andExpect(content().string(not(containsString(NUMBER))))
                .andExpect(content().string(not(containsString(HOLDER))));
    }

    @Test
    void passesTheIdempotencyKeyAndAnswersOkForAPaymentAlreadyMade() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, "order-42"))
                .thenReturn(new PaymentReceipt(WebFixtures.payment(PaymentStatus.APPROVED), false));

        mockMvc.perform(request(json(NUMBER, "123")).header("Idempotency-Key", "order-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PAYMENT_ID.toString()));
    }

    @Test
    void rejectsAnIdempotencyKeyLongerThan64Characters() throws Exception {
        mockMvc.perform(request(json(NUMBER, "123")).header("Idempotency-Key", "k".repeat(65)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(PaymentController.IDEMPOTENCY_KEY_MESSAGE));
        verifyNoInteractions(billingService);
    }

    @Test
    void rejectsMissingFields() throws Exception {
        pay("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subscriptionId").value("must not be null"))
                .andExpect(jsonPath("$.errors.card").value("must not be null"));
        pay("""
                {"subscriptionId":"%s","card":{}}""".formatted(SUBSCRIPTION_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['card.number']").value("must not be blank"))
                .andExpect(jsonPath("$.errors['card.holderName']").value("must not be blank"))
                .andExpect(jsonPath("$.errors['card.expiryMonth']").value("must not be null"))
                .andExpect(jsonPath("$.errors['card.expiryYear']").value("must not be null"))
                .andExpect(jsonPath("$.errors['card.cvv']").value("must not be blank"));
        verifyNoInteractions(billingService);
    }

    @Test
    void rejectsACvvThatIsNotThreeOrFourDigitsWithoutEchoingIt() throws Exception {
        pay(json(NUMBER, "12x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['card.cvv']").value("must be 3 or 4 digits"))
                .andExpect(content().string(not(containsString("12x"))))
                .andExpect(content().string(not(containsString(NUMBER))));
        verifyNoInteractions(billingService);
    }

    @Test
    void anInvalidCardIsABadRequestThatDoesNotEchoTheNumber() throws Exception {
        String invalid = "4111111111111112";
        when(billingService.pay(eq(OWNER_ID), eq(SUBSCRIPTION_ID), any(), isNull()))
                .thenThrow(new IllegalArgumentException("Card number is not valid"));

        pay(json(invalid, "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Card number is not valid"))
                .andExpect(content().string(not(containsString(invalid))));
    }

    @Test
    void aDeclinedPaymentAnswersPaymentRequired() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, null)).thenThrow(new PaymentRejectedException(
                WebFixtures.payment(PaymentStatus.REJECTED), Optional.of(DeclineReason.INSUFFICIENT_FUNDS)));

        pay(json(NUMBER, "123"))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.detail").value(PaymentRejectedException.MESSAGE))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID.toString()))
                .andExpect(jsonPath("$.declineReason").value("INSUFFICIENT_FUNDS"))
                .andExpect(content().string(not(containsString(NUMBER))));
    }

    @Test
    void aDeclinedPaymentAnsweredAgainHasNoDeclineReason() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, null)).thenThrow(new PaymentRejectedException(
                WebFixtures.payment(PaymentStatus.REJECTED), Optional.empty()));

        pay(json(NUMBER, "123"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.declineReason").doesNotExist());
    }

    @Test
    void anUnknownSubscriptionIsNotFound() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, null)).thenThrow(new SubscriptionNotFoundException());

        pay(json(NUMBER, "123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(SubscriptionNotFoundException.MESSAGE));
    }

    @Test
    void aSubscriptionThatCannotBePaidConflicts() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, null))
                .thenThrow(new SubscriptionNotPayableException(SubscriptionNotPayableException.CANCELLED));

        pay(json(NUMBER, "123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(SubscriptionNotPayableException.CANCELLED));
    }

    @Test
    void anIdempotencyKeyOfAnotherSubscriptionConflicts() throws Exception {
        when(billingService.pay(OWNER_ID, SUBSCRIPTION_ID, CARD, "order-42"))
                .thenThrow(new IdempotencyKeyConflictException());

        mockMvc.perform(request(json(NUMBER, "123")).header("Idempotency-Key", "order-42"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(IdempotencyKeyConflictException.MESSAGE));
    }

    @Test
    void answersUnauthorizedAndForbiddenBeforeReadingTheCard() throws Exception {
        mockMvc.perform(post(PAYMENTS).contentType(MediaType.APPLICATION_JSON).content(json(NUMBER, "123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PAYMENTS).header("X-User-Id", OWNER_ID.toString()).header("X-Role", "RESIDENT")
                        .contentType(MediaType.APPLICATION_JSON).content(json(NUMBER, "123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(NotAdministratorException.MESSAGE));
        verifyNoInteractions(billingService);
    }

    private ResultActions pay(String body) throws Exception {
        return mockMvc.perform(request(body));
    }

    private static MockHttpServletRequestBuilder request(String body) {
        return asAdministrator(post(PAYMENTS)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String json(String number, String cvv) {
        return """
                {"subscriptionId":"%s","card":{"number":"%s","holderName":"%s","expiryMonth":12,\
                "expiryYear":2030,"cvv":"%s"}}""".formatted(SUBSCRIPTION_ID, number, HOLDER, cvv);
    }
}
