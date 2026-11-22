package com.rumi.billing.infrastructure.web;

import com.rumi.billing.application.BuildingAlreadySubscribedException;
import com.rumi.billing.application.IdempotencyKeyConflictException;
import com.rumi.billing.application.PaymentRejectedException;
import com.rumi.billing.application.PlanNotFoundException;
import com.rumi.billing.application.SubscriptionNotFoundException;
import com.rumi.billing.application.SubscriptionNotPayableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

/**
 * Maps every error of the REST API to an RFC 7807 problem detail. Rejected values are never echoed back, so card
 * data cannot appear in an error response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CONFLICT_MESSAGE = "The request conflicts with the current state of the subscription.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem = exception.getBody();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(MissingUserIdException.class)
    public ProblemDetail handleMissingUserId(MissingUserIdException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(NotAdministratorException.class)
    public ProblemDetail handleNotAdministrator(NotAdministratorException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler({PlanNotFoundException.class, SubscriptionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({BuildingAlreadySubscribedException.class, SubscriptionNotPayableException.class,
            IdempotencyKeyConflictException.class})
    public ProblemDetail handleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    /**
     * The attempt is already stored as REJECTED; its id and the short reason code of the gateway are added.
     */
    @ExceptionHandler(PaymentRejectedException.class)
    public ProblemDetail handlePaymentRejected(PaymentRejectedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, exception.getMessage());
        problem.setProperty("paymentId", exception.payment().getId());
        exception.declineReason().ifPresent(reason -> problem.setProperty("declineReason", reason));
        return problem;
    }

    /**
     * A unique index rejected a write that raced with another request. The database message is not shown.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, CONFLICT_MESSAGE);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
}
