# rumi-billing-service

Billing service of **Rumi**, the structural monitoring platform by Kuntur Labs.

> **SKELETON: functionality planned for later sprints. The health endpoint is NOT counted as an implemented functional endpoint.**

| | |
|---|---|
| Bounded context | Billing |
| Port | `8087` |
| Gateway routes | `/api/v1/subscriptions/**`, `/api/v1/payments/**` |
| Base package | `com.rumi.billing` |

## Purpose

Will own subscription plans (US05), subscription status (US07) and online payments (US24).
None of it is implemented yet: this repository only fixes the service boundary, its port
and its place behind the API gateway.

## Origin

New service. It has no code in the modular monolith
[`rumi-backend`](https://github.com/upc-pre-202610-1asi0657-grupo4-Rumi/rumi-backend); it is one of the
bounded contexts of the target architecture defined when the monolith was decomposed.

## Endpoints

| Verb | Path | Description | Request | Response | User story | Status |
|---|---|---|---|---|---|---|
| GET | `/api/v1/subscriptions/health` | Check that the service is running | none | `200` `HealthResponse` | none | skeleton |

Implemented functional endpoints: 0. Skeleton endpoints: 1.

```json
{
  "status": "UP",
  "service": "rumi-billing-service"
}
```

## API documentation

- Swagger UI: <http://localhost:8087/swagger-ui.html>
- OpenAPI spec: <http://localhost:8087/v3/api-docs>
- Exported spec: [`docs/openapi.json`](docs/openapi.json)

## Run

Requirements: JDK 21 (the Maven wrapper is included; use `mvnw.cmd` on Windows).

```sh
./mvnw spring-boot:run
```

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8087` |

## Test

```sh
./mvnw test
```

## Structure

```
com.rumi.billing
├── domain             empty
├── application        empty
└── infrastructure     OpenAPI configuration
    └── web            health endpoint
```
