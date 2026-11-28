-- Seed of the dev profile (H2): the same two plans as docs/schema.sql.
INSERT INTO plans (id, code, name, billing_cycle, price, currency, sensor_limit) VALUES
  ('0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1001', 'MONTHLY', 'Plan mensual', 'MONTHLY',  199.00, 'PEN', 10),
  ('0b5c0f3e-4c5a-4f0e-9a51-2a6f2f7b1002', 'ANNUAL',  'Plan anual',   'ANNUAL',  1990.00, 'PEN', 10);
