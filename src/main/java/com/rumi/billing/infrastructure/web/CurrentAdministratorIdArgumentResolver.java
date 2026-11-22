package com.rumi.billing.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Resolves {@link CurrentAdministratorId} parameters from the {@code X-User-Id} and {@code X-Role} headers.
 */
public class CurrentAdministratorIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAdministratorId.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public UUID resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UUID userId = userId(webRequest.getHeader(CurrentAdministratorId.USER_ID_HEADER));
        if (!CurrentAdministratorId.ADMINISTRATOR.equals(webRequest.getHeader(CurrentAdministratorId.ROLE_HEADER))) {
            throw new NotAdministratorException();
        }
        return userId;
    }

    private static UUID userId(String header) {
        if (header == null || header.isBlank()) {
            throw new MissingUserIdException();
        }
        try {
            UUID userId = UUID.fromString(header.trim());
            // UUID.fromString accepts shortened forms such as "1-2-3-4-5"; only the canonical form is a real id.
            if (!userId.toString().equalsIgnoreCase(header.trim())) {
                throw new MissingUserIdException();
            }
            return userId;
        } catch (IllegalArgumentException malformed) {
            throw new MissingUserIdException();
        }
    }
}
