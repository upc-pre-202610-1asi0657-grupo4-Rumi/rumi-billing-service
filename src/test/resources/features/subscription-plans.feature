@US05
Feature: Subscription plans
  As the administrator of a building
  I want to choose a subscription plan (US05 Selección de plan de suscripción)
  So that my building can be monitored by Rumi

  Background:
    Given the administrator "Ana" is signed in through the gateway
    And the time is "2026-03-10T12:00:00Z"

  Scenario: The administrator lists the plans
    When "Ana" lists the subscription plans
    Then the response status is 200
    And the plans are "MONTHLY" at "199.00" PEN and "ANNUAL" at "1990.00" PEN, with 10 sensors each

  Scenario: The administrator chooses the monthly plan for a building
    When "Ana" chooses the plan "MONTHLY" for the building "Torre Lima"
    Then the response status is 201
    And the response is a subscription "PENDING_PAYMENT" to the plan "MONTHLY"
    And the response has the location of the subscription

  Scenario: A building cannot choose a second plan while the first one is pending payment
    Given "Ana" chose the plan "MONTHLY" for the building "Torre Lima"
    When "Ana" chooses the plan "ANNUAL" for the building "Torre Lima"
    Then the response status is 409
    And the response is a problem detail with the message "The building already has a subscription that is pending payment or active."

  Scenario: An unknown plan is not found
    When "Ana" chooses the plan "WEEKLY" for the building "Torre Lima"
    Then the response status is 404
    And the response is a problem detail with the message "The plan does not exist."

  Scenario: A building whose subscription has expired can choose a new plan
    Given "Ana" chose the plan "MONTHLY" for the building "Torre Lima"
    And "Ana" paid the subscription with the card "4111111111111111"
    And 32 days have passed
    When "Ana" chooses the plan "ANNUAL" for the building "Torre Lima"
    Then the response status is 201
    And the response is a subscription "PENDING_PAYMENT" to the plan "ANNUAL"
    And the first subscription of "Torre Lima" is stored as "EXPIRED"

  Scenario: A resident cannot see the plans
    Given the resident "Luis" is signed in through the gateway
    When "Luis" lists the subscription plans
    Then the response status is 403
    And the response is a problem detail with the message "Only an administrator can manage subscriptions and payments."
