package com.easytravel.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * Centralized, cross-service exception handling.
 *
 * <p>@RestControllerAdvice makes this a global interceptor: any exception thrown by
 * any @RestController in a service that component-scans this package is routed here,
 * turning stack traces into clean, consistent JSON. Because it lives in `common`,
 * all three services share ONE error-response shape for free.
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{

    /**
     * Handles Bean Validation failures (e.g. a blank userId on BookingRequestDTO).
     * Spring throws MethodArgumentNotValidException when a @Valid @RequestBody fails;
     * we unpack each field error into a readable map and return 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request)
    {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /**
     * Safety net for anything not caught by a more specific handler. Returns 500 but
     * deliberately does NOT leak the exception's internals to the caller — we expose
     * a generic message and rely on server-side logging for the detail.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, WebRequest request)
    {
        // TEMPORARY DEBUGGING: print the real cause to the console so we can see it.
        // Remove before the phase is done; a real system logs at ERROR with a correlation id.
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    /**
     * Assembles a uniform error body. One shape, every service, every error.
     */
    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String message, Map<String, String> details)
    {
        Map<String, Object> body = new HashMap<>();
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