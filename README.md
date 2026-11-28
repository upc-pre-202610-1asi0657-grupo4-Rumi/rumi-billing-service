# rumi-billing-service

Billing service of **Rumi**, the structural monitoring platform by Kuntur Labs.

| | |
|---|---|
| Bounded context | Billing |
| Port | `8087` |
| Gateway routes | `/api/v1/subscriptions/**`, `/api/v1/payments/**` (administrators only) |
| Base package | `com.rumi.billing` |
| Database | PostgreSQL `billingdb` (in-memory H2 with the `dev` profile) |

## Purpose

Lets the administrator of a building choose a subscription plan (US05), see the status of the subscription and its
payment history (US07) and pay it online (US24). Choosing a plan creates the subscription as `PENDING_PAYMENT`; an
approved payment activates it, renews it or reactivates it.

The report calls this database "SubscriptionsDB"; it is `billingdb`, the same way IAM uses `iamdb`.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

Every route needs the token of an administrator. The API gateway validates it, restricts both prefixes to the
`ADMINISTRATOR` role and passes `X-User-Id` and `X-Role` to the service. The service checks them again (defence in
depth): a missing or malformed `X-User-Id` gives `401`, a role other than `ADMINISTRATOR` gives `403`.

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| GET | `/api/v1/subscriptions/plans` | List the subscription plans | none | `200` `PlanResponse[]` | US05 | implemented |
| POST | `/api/v1/subscriptions` | Choose a plan for a building | `ChooseSubscriptionRequest` | `201` `SubscriptionResponse` + `Location` | US05 | implemented |
| GET | `/api/v1/subscriptions/{subscriptionId}` | Read a subscription: plan, status, expiry | none | `200` `SubscriptionResponse` | US07 | implemented |
| GET | `/api/v1/subscriptions/{subscriptionId}/payments` | List the payments of a subscription, newest first | none | `200` `PaymentResponse[]` | US07 | implemented |
| POST | `/api/v1/payments` | Pay a subscription online | `PaymentRequest`, optional `Idempotency-Key` header | `201` / `200` `PaymentResponse` | US24 | implemented |
| GET | `/api/v1/subscriptions/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 5. Skeleton endpoints: 1.

### Plans (US05)

```json
GET /api/v1/subscriptions/plans
200 OK
[
  {"id": "0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001", "code": "MONTHLY", "name": "Plan mensual",
   "billingCycle": "MONTHLY", "price": 199.00, "currency": "PEN", "sensorLimit": 10},
  {"id": "0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002", "code": "ANNUAL", "name": "Plan anual",
   "billingCycle": "ANNUAL", "price": 1990.00, "currency": "PEN", "sensorLimit": 10}
]
```

Plans are reference data created by [`docs/schema.sql`](docs/schema.sql) (and `db/dev-plans.sql` in the `dev`
profile):

| Plan | Price (PEN, IGV included) | Sensors |
|---|---|---|
| `MONTHLY` | 199.00 per month | 10 |
| `ANNUAL` | 1990.00 per year | 10 |

Hardware and installation are quoted separately. The model has no tax field: the price already includes IGV.
`sensorLimit` is informative for now; no service enforces it yet.

The seed only runs when the database is created. To update a database created with the earlier prices
(149.00 / 1490.00 PEN, 20 sensors):

```sql
UPDATE plans SET price = 199.00, sensor_limit = 10 WHERE code = 'MONTHLY';
UPDATE plans SET price = 1990.00, sensor_limit = 10 WHERE code = 'ANNUAL';
```

### Choose a plan (US05)

```json
POST /api/v1/subscriptions
{
  "buildingId": "3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b",
  "planCode": "MONTHLY"
}
```

```json
201 Created
Location: /api/v1/subscriptions/9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d
{
  "id": "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d",
  "buildingId": "3f1a2b3c-4d5e-4f60-8a7b-9c0d1e2f3a4b",
  "plan": {"id": "0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001", "code": "MONTHLY", "...": "..."},
  "status": "PENDING_PAYMENT",
  "startsAt": null,
  "expiresAt": null,
  "createdAt": "2026-11-21T15:00:00Z"
}
```

- An unknown plan code gives `404`.
- A building has **one open subscription** at most (`PENDING_PAYMENT` or `ACTIVE`): choosing again gives `409`. It is
  checked by the application and, in PostgreSQL, by the partial unique index `uq_subscriptions_open_building`.
- If the open subscription of the building is `ACTIVE` but its expiry has passed, it is stored as `EXPIRED` at that
  moment and the new subscription is created.

### Subscription status and payment history (US07)

`GET /api/v1/subscriptions/{subscriptionId}` answers the `SubscriptionResponse` above with `status`, `startsAt` and
`expiresAt`. `GET .../payments` lists every attempt, approved or rejected, newest first:

```json
200 OK
[
  {"id": "1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f", "subscriptionId": "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d",
   "amount": 199.00, "currency": "PEN", "status": "APPROVED", "cardBrand": "VISA", "cardLast4": "1111",
   "paidAt": "2026-11-21T15:05:00Z"}
]
```

- A subscription of another administrator gets the same `404` as one that does not exist.
- There is no scheduler yet: an `ACTIVE` subscription whose `expiresAt` has passed is **reported** as `EXPIRED`
  when it is read (computed from the clock), and stored as `EXPIRED` only when its building chooses a new plan.
- `paidAt` is the time of the attempt, also for rejected ones.

### Online payment (US24)

```json
POST /api/v1/payments
Idempotency-Key: order-2026-11-21-0001        (optional, at most 64 characters)
{
  "subscriptionId": "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d",
  "card": {
    "number": "4111111111111111",
    "holderName": "Ana Torres",
    "expiryMonth": 12,
    "expiryYear": 2030,
    "cvv": "123"
  }
}
```

```json
201 Created
{
  "id": "1c2d3e4f-5a6b-4c7d-8e9f-0a1b2c3d4e5f",
  "subscriptionId": "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d",
  "amount": 199.00,
  "currency": "PEN",
  "status": "APPROVED",
  "cardBrand": "VISA",
  "cardLast4": "1111",
  "paidAt": "2026-11-21T15:05:00Z"
}
```

- The price of the plan is charged. An approved payment:
  - on a `PENDING_PAYMENT` subscription starts it now, for one cycle;
  - on an `ACTIVE` one renews it for one cycle from its current `expiresAt`;
  - on an expired one restarts it from now (`409` if its building has already chosen a new subscription).
- Cycles use calendar months in UTC: 31 January + 1 month = 28 (or 29) February.
- A declined payment is stored as `REJECTED` (it appears in the history), the subscription does not change, and the
  answer is `402` with the short reason code of the gateway:

  ```json
  {"type": "about:blank", "title": "Payment Required", "status": 402, "detail": "The payment was declined.",
   "instance": "/api/v1/payments", "paymentId": "a9cc1bfd-d567-41ea-a646-4da0ce37821c",
   "declineReason": "INSUFFICIENT_FUNDS"}
  ```

- The card is validated before anything else: Luhn check, not expired (valid until the end of its expiry month), CVV
  of 3 digits (4 for AMEX). Otherwise `400`. A cancelled subscription gives `409`.
- **`Idempotency-Key`**: repeating a key for the same subscription answers `200` with the stored payment and charges
  nothing; a key already used for another subscription gives `409`. The key of a rejected attempt answers `402` again
  (without `declineReason`, which is not stored); send a new key to retry with another card. Payments of the same
  subscription are serialized by a row lock, and a key stored by a racing request is caught on the unique index and
  answered with the stored payment.
- **Card data is never stored, logged or returned.** Only the brand and the last four digits are kept. A test runs
  the whole flow on PostgreSQL and checks that the card number and the holder name appear in no response, no log line
  and no text column of any table, and that no column holds the CVV.

#### Simulated payment gateway: test cards

The gateway is reached through the Anti-Corruption Layer port `PaymentGatewayClient`. Its only adapter today is
`SimulatedPaymentGatewayClient`: nothing is charged and the card never leaves the process. A real gateway (Culqi,
Stripe) would be a second adapter and would change no domain code.

| Card number | Result |
|---|---|
| `4000000000000002` | Declined, `INSUFFICIENT_FUNDS` (`402`) |
| `4111111111111111` | Approved, `VISA` |
| `5500000000000004` | Approved, `MASTERCARD` |
| `378282246310005` (CVV of 4 digits) | Approved, `AMEX` |
| Any other number that passes Luhn | Approved; brand by prefix (4 VISA, 51-55 MASTERCARD, 34/37 AMEX, otherwise UNKNOWN) |
| A number that fails Luhn, e.g. `4111111111111112` | `400 Card number is not valid` |

Each charge gets a reference `sim_<uuid>`, stored with the payment but not returned.

### Errors

Every error is an RFC 7807 problem detail (`application/problem+json`). Validation errors list the fields in `errors`;
rejected values are never echoed back.

| Status | When |
|---|---|
| `400` | Invalid data (`errors` map), malformed body or id, invalid card, `Idempotency-Key` longer than 64 characters |
| `401` | No valid `X-User-Id` header: the request did not come through the gateway |
| `402` | The gateway declined the payment |
| `403` | `X-Role` is not `ADMINISTRATOR` |
| `404` | Unknown plan, or a subscription that does not exist or belongs to another administrator |
| `409` | The building already has an open subscription; the subscription is cancelled or replaced; the `Idempotency-Key` belongs to another subscription |

> **Billing trusts the gateway headers.** The gateway removes any `X-User-Id` and `X-Role` sent by a client and sets
> them from a valid token, so in a real deployment Billing must only be reachable through the gateway.

## Known limitations

- **The building is not checked.** Billing does not call Building: `buildingId` is accepted as given, and its
  ownership cannot be checked yet because the `buildingIds` claim of the token is still empty.
- No scheduler moves `ACTIVE` subscriptions to `EXPIRED` or sends renewal reminders.
- No endpoint cancels a subscription yet (the `CANCELLED` status exists in the model and the schema).
- The charge runs inside the database transaction, which is fine for the simulated gateway only (see the comment on
  `BillingApplicationService.pay`).

## API documentation

- Swagger UI: <http://localhost:8087/swagger-ui.html> (every response code has an example)
- OpenAPI spec: <http://localhost:8087/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)
- Database schema: [`docs/schema.sql`](docs/schema.sql)

## Run

Requirements: JDK 21 (the Maven wrapper is included; use `mvnw.cmd` on Windows).

Without any infrastructure, on an in-memory H2 database seeded with the two plans:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
curl -H "X-User-Id: 7d3c5b1e-2f4a-4c8e-9b6d-1a2b3c4d5e6f" -H "X-Role: ADMINISTRATOR" \
     http://localhost:8087/api/v1/subscriptions/plans
```

H2 has no partial index, so in `dev` the "one open subscription per building" rule is enforced by the application
only.

With PostgreSQL (default profile): create `billingdb` and apply [`docs/schema.sql`](docs/schema.sql) first (the
compose file of `rumi-backend` does it).

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8087` |
| `BILLING_DB_URL` | `jdbc:postgresql://localhost:5432/billingdb` |
| `BILLING_DB_USERNAME` | `postgres` |
| `BILLING_DB_PASSWORD` | `postgres` |

Billing does not parse tokens, so it needs no `JWT_SECRET`.

## Test

```sh
./mvnw test
```

Runs the domain and application unit tests, the MockMvc and JPA slice tests, the end-to-end payment flow test and
the Cucumber scenarios of [`src/test/resources/features`](src/test/resources/features), tagged `@US05`, `@US07` and
`@US24`. The scenarios are printed to the console and written to `target/cucumber-report.html`.

**Docker must be running.** The JPA tests, the Cucumber scenarios and the context tests share one PostgreSQL 16
container (Testcontainers), created from [`docs/schema.sql`](docs/schema.sql) with Hibernate in `validate` mode, so the
build fails if the schema and the JPA entities disagree. `src/test/resources/docker-java.properties` pins Docker API
1.44, which Docker Engine 25 and later accept (Engine 29 rejects the older default of the client).

## Structure

```
com.rumi.billing
├── domain
│   ├── model          Subscription (aggregate root of Payment), Payment, Plan, Money, BillingCycle,
│   │                  SubscriptionStatus, PaymentStatus, CardDetails, CardBrand
│   ├── repository     PlanRepository, SubscriptionRepository
│   └── service        PaymentGatewayClient (Anti-Corruption Layer port), ChargeRequest, GatewayResult,
│                      DeclineReason
├── application        BillingApplicationService and its exceptions
└── infrastructure     OpenAPI and clock configuration
    ├── persistence    JPA entities and repositories
    ├── payment        SimulatedPaymentGatewayClient
    └── web            subscription, payment and health endpoints, gateway header resolver, DTOs,
                       problem details
```

## Follow-ups

- Check that the building exists, and that it belongs to the administrator, once IAM fills `buildingIds`.
- A scheduler that moves `ACTIVE` to `EXPIRED` and sends a renewal reminder through Notification.
- A real payment gateway adapter with webhook confirmation.
- Cancel subscription endpoint.
