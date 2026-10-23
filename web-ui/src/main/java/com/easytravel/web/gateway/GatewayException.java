package com.easytravel.web.gateway;

/**
 * WHAT:   A call to the API gateway that did not succeed.
 * WHY:    One exception type for every failure (4xx, 5xx, unreachable), so the
 *         pages need one catch and can show the message as it is.
 *
 * @author AoceanP
 */
public class GatewayException extends RuntimeException
{
    /** WHAT: the HTTP status, or 503 when the gateway could not be reached. */
    private final int status;

    /**
     * WHAT:   Creates the exception.
     * PRE:    message is safe to show to a user
     *
     * @param status  the HTTP status
     * @param message what went wrong, in plain words
     */
    public GatewayException(int status, String message)
    {
        super(message);
        this.status = status;
    }

    /**
     * WHAT:   The HTTP status of the failed call.
     * PRE:    none
     * RETURN: the status
     *
     * @return the status
     */
    public int getStatus()
    {
        return status;
    }
}
