package com.easytravel.common.exception;

/**
 * WHAT:   A request that is valid but not allowed in the resource's current
 *         state, e.g. cancelling a booking that is still waiting for payment.
 * WHY:    Answered as 409 Conflict by GlobalExceptionHandler, so the client
 *         knows the request was understood but the timing or state is wrong.
 *
 * @author AoceanP
 */
public class ConflictException extends RuntimeException
{
    /**
     * WHAT:   Creates the exception.
     * PRE:    message is safe to show the client
     *
     * @param message what conflicts, in plain words
     */
    public ConflictException(String message)
    {
        super(message);
    }
}
