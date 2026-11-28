package com.rumi.billing.infrastructure.web;

/**
 * Texts and examples shared by the OpenAPI annotations of the Billing controllers.
 */
final class ApiDocumentation {

    static final String USER_ID_DESCRIPTION =
            "Id of the signed-in administrator, set by the API gateway from the token; clients do not send it";
    static final String USER_ID_EXAMPLE = "7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f";
    static final String ROLE_DESCRIPTION =
            "Role of the signed-in user, set by the API gateway from the token; it must be ADMINISTRATOR";
    static final String GATEWAY_NOTE = " Call it through the API gateway with 'Authorization: Bearer <token>' of an "
            + "administrator; the gateway sets X-User-Id and X-Role.";

    static final String UNAUTHORIZED_EXAMPLE = """
            {"type":"about:blank","title":"Unauthorized","status":401,\
            "detail":"The request has no valid X-User-Id header. Call this endpoint through the API gateway with a \
            valid token.","instance":"/api/v1/subscriptions/plans"}""";
    static final String FORBIDDEN_EXAMPLE = """
            {"type":"about:blank","title":"Forbidden","status":403,\
            "detail":"Only an administrator can manage subscriptions and payments.",\
            "instance":"/api/v1/subscriptions/plans"}""";
    static final String SUBSCRIPTION_NOT_FOUND_EXAMPLE = """
            {"type":"about:blank","title":"Not Found","status":404,\
            "detail":"The subscription does not exist.",\
            "instance":"/api/v1/subscriptions/9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d"}""";

    static final String SUBSCRIPTION_EXAMPLE = """
            {"id":"9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d","buildingId":"3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b",\
            "plan":{"id":"0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001","code":"MONTHLY","name":"Plan mensual",\
            "billingCycle":"MONTHLY","price":199.00,"currency":"PEN","sensorLimit":10},\
            "status":"ACTIVE","startsAt":"2026-11-21T15:05:00Z","expiresAt":"2026-12-21T15:05:00Z",\
            "createdAt":"2026-11-21T15:00:00Z"}""";
    static final String PAYMENT_EXAMPLE = """
            {"id":"1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f","subscriptionId":"9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d",\
            "amount":199.00,"currency":"PEN","status":"APPROVED","cardBrand":"VISA","cardLast4":"1111",\
            "paidAt":"2026-11-21T15:05:00Z"}""";

    private ApiDocumentation() {
    }
}
