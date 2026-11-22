package com.rumi.billing.infrastructure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the id of the signed-in administrator. rumi-api-gateway validates the token, removes any such headers sent
 * by the client and sets {@code X-User-Id} from the {@code sub} claim and {@code X-Role} from the {@code role}
 * claim. The gateway already allows only administrators on Billing routes; the service checks it again (defence in
 * depth): a missing or malformed {@code X-User-Id} gives 401 and a role other than {@code ADMINISTRATOR} gives 403.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAdministratorId {

    String USER_ID_HEADER = "X-User-Id";
    String ROLE_HEADER = "X-Role";
    String ADMINISTRATOR = "ADMINISTRATOR";
}
