package com.rumi.billing.infrastructure.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.rumi.billing.infrastructure.web.ApiDocumentation.ROLE_DESCRIPTION;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.USER_ID_DESCRIPTION;
import static com.rumi.billing.infrastructure.web.ApiDocumentation.USER_ID_EXAMPLE;

/**
 * Documents the two headers that the API gateway sets on every Billing request.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(name = CurrentAdministratorId.USER_ID_HEADER, in = ParameterIn.HEADER, required = true,
        description = USER_ID_DESCRIPTION, example = USER_ID_EXAMPLE)
@Parameter(name = CurrentAdministratorId.ROLE_HEADER, in = ParameterIn.HEADER, required = true,
        description = ROLE_DESCRIPTION, example = CurrentAdministratorId.ADMINISTRATOR)
@interface GatewayHeaders {
}
