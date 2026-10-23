package com.easytravel.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * WHAT:   Turns exceptions from any controller into one consistent JSON error shape.
 * WHY:    @RestControllerAdvice intercepts exceptions from every
 *         @RestController in a service that scans this package. Living in
 *         common, it gives every service the same error format, and it never
 *         leaks stack traces or internal messages to the client.
 *
 *         { "timestamp": ..., "status": 404, "error": "Not Found", "message": "booking 42 not found" }
 *
 * @author AoceanP
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /** WHAT: logs unexpected errors with their stack trace, on the server only. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * WHAT:   Bean Validation failures on a @Valid request body, e.g. a blank userId.
     * WHY:    Lists every invalid field at once, so the client can fix them all in one go.
     * PRE:    Spring threw MethodArgumentNotValidException
     * RETURN: 400 with a field-to-message map, sorted by field name
     *
     * @param ex the validation exception
     * @return the error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex)
    {
        Map<String, String> fieldErrors = new TreeMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /**
     * WHAT:   A body that is not valid JSON, or an unknown enum value such as fareClass "PREMIUM".
     * WHY:    Answers 400 instead of 500; the detail stays in the server log,
     *         because parser messages can reveal internal class names.
     * PRE:    Spring could not read the request body
     * RETURN: 400
     *
     * @param ex the parsing exception
     * @return the error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex)
    {
        log.debug("unreadable request body", ex);
        return build(HttpStatus.BAD_REQUEST, "The request body is not valid JSON, or has an unknown value", null);
    }

    /**
     * WHAT:   A path or query parameter of the wrong type, e.g. /api/bookings/abc.
     * PRE:    Spring could not convert a parameter
     * RETURN: 400 naming the parameter
     *
     * @param ex the conversion exception
     * @return the error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex)
    {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'", null);
    }

    /**
     * WHAT:   A required query parameter is missing, e.g. a search with no destination.
     * PRE:    Spring found no value for a required @RequestParam
     * RETURN: 400 naming the parameter
     *
     * @param ex the missing-parameter exception
     * @return the error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex)
    {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter '" + ex.getParameterName() + "'", null);
    }

    /**
     * WHAT:   A query parameter broke a constraint on a @Validated controller, e.g. origin=lhr.
     * WHY:    Method validation reports these separately from request-body
     *         validation; without this handler they fall through to a 500.
     * PRE:    Bean Validation rejected a method argument
     * RETURN: 400 with one message per broken constraint
     *
     * @param ex the validation exception
     * @return the error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex)
    {
        Map<String, String> details = new TreeMap<>();
        ex.getConstraintViolations().forEach(v -> details.put(v.getPropertyPath().toString(), v.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /**
     * WHAT:   The same as above, for Spring's built-in method validation.
     * WHY:    Spring 6.1+ can validate controller parameters itself, and then
     *         raises this type instead of ConstraintViolationException.
     * PRE:    Spring rejected a controller method argument
     * RETURN: 400 with one message per broken constraint
     *
     * @param ex the validation exception
     * @return the error response
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException ex)
    {
        Map<String, String> details = new TreeMap<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                details.put(result.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /**
     * WHAT:   Something that does not exist was requested.
     * PRE:    a service threw ResourceNotFoundException
     * RETURN: 404 with the exception's message
     *
     * @param ex the exception
     * @return the error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex)
    {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    /**
     * WHAT:   A service this one depends on is down.
     * PRE:    a service threw ServiceUnavailableException
     * RETURN: 503, so the client knows to retry later
     *
     * @param ex the exception
     * @return the error response
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleUnavailable(ServiceUnavailableException ex)
    {
        log.warn("{}", ex.getMessage(), ex.getCause());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }

    /**
     * WHAT:   The safety net for anything else.
     * WHY:    Logs the full error on the server, but tells the client only
     *         "unexpected error": exception messages can leak internals.
     * PRE:    no more specific handler matched
     * RETURN: 500 with a generic message
     *
     * @param ex the exception
     * @return the error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex)
    {
        log.error("unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    /**
     * WHAT:   Builds the uniform error body.
     * WHY:    One shape for every service and every error, in a fixed key order.
     * PRE:    status and message are not null
     * RETURN: the response
     *
     * @param status  the HTTP status
     * @param message a message that is safe to show the client
     * @param details extra field errors, or null
     * @return the response
     */
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, Map<String, String> details)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null)
        {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
