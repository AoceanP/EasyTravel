package com.easytravel.common.exception;

/**
 * WHAT:   Thrown when a requested booking, payment or flight does not exist.
 * WHY:    GlobalExceptionHandler turns it into 404 Not Found; before, a
 *         missing booking came back as 500 Internal Server Error.
 *
 * @author AoceanP
 */
public class ResourceNotFoundException extends RuntimeException
{
    /**
     * WHAT:   Creates the exception.
     * PRE:    message names what was not found
     *
     * @param message e.g. "booking 42 not found"
     */
    public ResourceNotFoundException(String message)
    {
        super(message);
    }
}
