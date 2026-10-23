package com.easytravel.common.exception;

/**
 * WHAT:   Thrown when a service this one depends on cannot be reached.
 * WHY:    GlobalExceptionHandler turns it into 503 Service Unavailable, which
 *         tells the client "try again later" instead of "you did something wrong".
 *
 * @author AoceanP
 */
public class ServiceUnavailableException extends RuntimeException
{
    /**
     * WHAT:   Creates the exception.
     * PRE:    message names the unavailable service
     *
     * @param message e.g. "inventory-service is unavailable"
     * @param cause   the underlying error
     */
    public ServiceUnavailableException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
