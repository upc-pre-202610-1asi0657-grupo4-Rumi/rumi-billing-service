-- Schema of billingdb, the database of the Billing bounded context (called "SubscriptionsDB" in the report).
-- It is loaded by the PostgreSQL container of the tests and by deploy/postgres/init in rumi-backend.
-- Hibernate only validates it (ddl-auto=validate).

CREATE TABLE plans (
    id            uuid          PRIMARY KEY,
    code          varchar(20)   NOT NULL UNIQUE,
    name          varchar(60)   NOT NULL,
    billing_cycle varchar(10)   NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'ANNUAL')),
    price         numeric(10,2) NOT NULL CHECK (price > 0),
    currency      char(3)       NOT NULL,
    sensor_limit  integer       NOT NULL CHECK (sensor_limit > 0)
);

CREATE TABLE subscriptions (
    id          uuid        PRIMARY KEY,
    owner_id    uuid        NOT NULL,   -- user of IAM, no foreign key across services
    building_id uuid        NOT NULL,   -- building of Building, no foreign key across services
    plan_id     uuid        NOT NULL REFERENCES plans (id),
    status      varchar(20) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    starts_at   timestamp,
    expires_at  timestamp,
    created_at  timestamp   NOT NULL
);
-- A building has at most one open subscription.
CREATE UNIQUE INDEX uq_subscriptions_open_building ON subscriptions (building_id)
    WHERE status IN ('PENDING_PAYMENT', 'ACTIVE');
CREATE INDEX idx_subscriptions_owner ON subscriptions (owner_id);

-- Card data is never stored: only the brand and the last four digits.
CREATE TABLE payments (
    id                uuid          PRIMARY KEY,
    subscription_id   uuid          NOT NULL REFERENCES subscriptions (id),
    amount            numeric(10,2) NOT NULL,
    currency          char(3)       NOT NULL,
    status            varchar(10)   NOT NULL CHECK (status IN ('APPROVED', 'REJECTED')),
    card_brand        varchar(12)   NOT NULL,
    card_last4        char(4)       NOT NULL,
    gateway_reference varchar(64)   NOT NULL,
    idempotency_key   varchar(64)   UNIQUE,
    created_at        timestamp     NOT NULL
);
CREATE INDEX idx_payments_subscription ON payments (subscription_id, created_at DESC);

-- Reference data. Prices and sensor limits are assumptions of the team.
-- Prices in PEN with IGV included; hardware and installation are quoted separately.
INSERT INTO plans (id, code, name, billing_cycle, price, currency, sensor_limit) VALUES
  ('0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001', 'MONTHLY', 'Plan mensual', 'MONTHLY',  199.00, 'PEN', 10),
  ('0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002', 'ANNUAL',  'Plan anual',   'ANNUAL',  1990.00, 'PEN', 10);
