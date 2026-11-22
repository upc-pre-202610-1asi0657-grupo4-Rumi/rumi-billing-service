package com.rumi.billing.acceptance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a scenario remembers between steps: the users signed in through the gateway, the buildings by name and
 * the subscriptions chosen for them, and the last card sent.
 */
public class ScenarioState {

    private final Map<String, Map<String, String>> gatewayHeaders = new HashMap<>();
    private final Map<String, UUID> buildings = new HashMap<>();
    private final Map<String, List<UUID>> subscriptionsByBuilding = new HashMap<>();
    private UUID subscriptionId;
    private String lastCardNumber;

    public void signIn(String name, String role) {
        gatewayHeaders.put(name, Map.of("X-User-Id", UUID.randomUUID().toString(), "X-Role", role));
    }

    public Map<String, String> headersOf(String name) {
        Map<String, String> headers = gatewayHeaders.get(name);
        if (headers == null) {
            throw new IllegalStateException(name + " is not signed in");
        }
        return headers;
    }

    public UUID building(String name) {
        return buildings.computeIfAbsent(name, ignored -> UUID.randomUUID());
    }

    public void subscriptionChosen(String building, UUID id) {
        subscriptionsByBuilding.computeIfAbsent(building, ignored -> new ArrayList<>()).add(id);
        subscriptionId = id;
    }

    public UUID firstSubscriptionOf(String building) {
        return subscriptionsByBuilding.get(building).get(0);
    }

    public UUID subscriptionId() {
        if (subscriptionId == null) {
            throw new IllegalStateException("No subscription was chosen in this scenario");
        }
        return subscriptionId;
    }

    public String lastCardNumber() {
        return lastCardNumber;
    }

    public void cardSent(String number) {
        lastCardNumber = number;
    }
}
