@US07
Feature: Subscription status
  As the administrator of a building
  I want to see the status of my subscription (US07 Consulta del estado de suscripción)
  So that I know its plan, when it expires and what I have paid

  Background:
    Given the administrator "Ana" is signed in through the gateway
    And the time is "2026-03-10T12:00:00Z"
    And "Ana" chose the plan "MONTHLY" for the building "Torre Lima"

  Scenario: After an approved payment the administrator sees the plan, the expiry and the history
    Given "Ana" paid the subscription with the card "4111111111111111"
    When "Ana" reads the subscription
    Then the response status is 200
    And the response is a subscription "ACTIVE" to the plan "MONTHLY"
    And the subscription expires at "2026-04-10T12:00:00Z"
    When "Ana" lists the payments of the subscription
    Then the response status is 200
    And the payment history is "APPROVED"

  Scenario: Another administrator cannot see the subscription
    Given the administrator "Rosa" is signed in through the gateway
    When "Rosa" reads the subscription
    Then the response status is 404
    And the response is a problem detail with the message "The subscription does not exist."
    When "Rosa" lists the payments of the subscription
    Then the response status is 404

  Scenario: The history lists a rejected attempt too
    Given "Ana" tried to pay the subscription with the card "4000000000000002"
    And 1 days have passed
    And "Ana" paid the subscription with the card "4111111111111111"
    When "Ana" lists the payments of the subscription
    Then the response status is 200
    And the payment history is "APPROVED, REJECTED"

  Scenario: An active subscription past its expiry is reported as expired
    Given "Ana" paid the subscription with the card "4111111111111111"
    And 31 days have passed
    When "Ana" reads the subscription
    Then the response status is 200
    And the response is a subscription "EXPIRED" to the plan "MONTHLY"
