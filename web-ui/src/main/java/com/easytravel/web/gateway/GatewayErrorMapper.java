package com.easytravel.web.gateway;

import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

/**
 * WHAT:   Turns an error response from the gateway into a GatewayException.
 * WHY:    The services already explain their errors in a JSON body; reading it
 *         here means the user sees "flight not found", not "HTTP 404".
 *
 * @author AoceanP
 */
public class GatewayErrorMapper implements ResponseExceptionMapper<GatewayException>
{
    /**
     * WHAT:   Builds the exception from the response.
     * WHY:    A body that is missing or not the usual shape still gives a
     *         sensible message instead of a second error.
     * PRE:    the response is an error (status 400 or higher)
     * RETURN: the exception to throw
     *
     * @param response the error response
     * @return the exception
     */
    @Override
    public GatewayException toThrowable(Response response)
    {
        String message = null;
        try
        {
            ApiError body = response.readEntity(ApiError.class);
            message = body == null ? null : body.message();
        }
        catch (RuntimeException unreadable)
        {
            // not the usual error body: fall back to a generic message below
        }
        int status = response.getStatus();
        if (message == null || message.isBlank())
        {
            message = status >= 500 ? "The booking system is unavailable right now. Try again shortly."
                                    : "The request was not accepted (HTTP " + status + ").";
        }
        return new GatewayException(status, message);
    }
}
