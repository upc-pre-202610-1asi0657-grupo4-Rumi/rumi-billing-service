package com.rumi.billing.infrastructure.web;

/**
 * Raised when the {@code X-Role} header set by the API gateway is not {@code ADMINISTRATOR}.
 */
public class NotAdministratorException extends RuntimeException {

    public static final String MESSAGE = "Only an administrator can manage subscriptions and payments.";

    public NotAdministratorException() {
        super(MESSAGE);
    }
}
