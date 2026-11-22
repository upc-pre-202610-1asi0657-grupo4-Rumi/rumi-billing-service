@US24
Feature: Online payment
  As the administrator of a building
  I want to pay my subscription online (US24 Pago en línea de la suscripción)
  So that it is activated or renewed without paperwork

  Background:
    Given the administrator "Ana" is signed in through the gateway
    And the time is "2026-03-10T12:00:00Z"
    And "Ana" chose the plan "MONTHLY" for the building "Torre Lima"

  Scenario: A valid card activates the subscription for one month
    When "Ana" pays the subscription with the card "4111111111111111"
    Then the response status is 201
    And the payment is "APPROVED" with a "VISA" card ending in "1111"
    And the response does not contain the card
    And the subscription is "ACTIVE" until "2026-04-10T12:00:00Z"

  Scenario: A second payment renews the subscription from its current expiry
    Given "Ana" paid the subscription with the card "4111111111111111"
    And 10 days have passed
    When "Ana" pays the subscription with the card "5500000000000004"
    Then the response status is 201
    And the payment is "APPROVED" with a "MASTERCARD" card ending in "0004"
    And the subscription is "ACTIVE" until "2026-05-10T12:00:00Z"

  Scenario: A declined card is answered with 402 and the subscription stays pending payment
    When "Ana" pays the subscription with the card "4000000000000002"
    Then the response status is 402
    And the response is a problem detail with the message "The payment was declined."
    And the decline reason is "INSUFFICIENT_FUNDS"
    And the response does not contain the card
    And the subscription is "PENDING_PAYMENT"

  Scenario: An invalid card number is a bad request
    When "Ana" pays the subscription with the card "4111111111111112"
    Then the response status is 400
    And the response is a problem detail with the message "Card number is not valid"
    And the response does not contain the card
    And the subscription is "PENDING_PAYMENT"

  Scenario: The same Idempotency-Key sent twice charges once
    When "Ana" pays the subscription with the card "4111111111111111" and the idempotency key "order-0001"
    Then the response status is 201
    When "Ana" pays the subscription with the card "4111111111111111" and the idempotency key "order-0001"
    Then the response status is 200
    And the subscription has 1 payment
    And the subscription is "ACTIVE" until "2026-04-10T12:00:00Z"

  Scenario: Paying an expired subscription reactivates it from now
    Given "Ana" paid the subscription with the card "4111111111111111"
    And 40 days have passed
    When "Ana" pays the subscription with the card "4111111111111111"
    Then the response status is 201
    And the subscription is "ACTIVE" until "2026-05-19T12:00:00Z"
