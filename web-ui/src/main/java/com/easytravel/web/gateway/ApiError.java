package com.easytravel.web.gateway;

import java.util.Map;

/**
 * WHAT:   The error body every EasyTravel service returns (GlobalExceptionHandler).
 * WHY:    Lets the website show the server's own message, e.g.
 *         "flight ZZ999 from LHR to JFK not found", instead of a bare status code.
 *
 * @param status  the HTTP status
 * @param error   the status name
 * @param message what went wrong
 * @param details one message per bad field, for validation errors
 * @author AoceanP
 */
public record ApiError(int status, String error, String message, Map<String, String> details)
{
}
