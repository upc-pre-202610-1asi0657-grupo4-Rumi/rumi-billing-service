package com.rumi.billing.application;

/**
 * Raised when a subscription asks for a plan code that does not exist (404).
 */
public class PlanNotFoundException extends RuntimeException {

    public static final String MESSAGE = "The plan does not exist.";

    public PlanNotFoundException() {
        super(MESSAGE);
    }
}
