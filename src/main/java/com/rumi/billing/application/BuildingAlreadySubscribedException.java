package com.rumi.billing.application;

/**
 * Raised when a building that already has an open subscription chooses a plan again (409).
 */
public class BuildingAlreadySubscribedException extends RuntimeException {

    public static final String MESSAGE = "The building already has a subscription that is pending payment or active.";

    public BuildingAlreadySubscribedException() {
        super(MESSAGE);
    }
}
