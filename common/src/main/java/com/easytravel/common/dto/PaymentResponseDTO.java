package com.easytravel.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 *
 * @author aleksandar panich
 * @version 1.0
 *
 * Immutable contract describing the outcome of a payment attempt.
 *
 * <p>payment-service produces this. In Phase 3.2 the same information travels as an
 * event over RabbitMQ so booking-service can flip a booking to CONFIRMED.
 *
 * @param transactionId unique id for the payment attempt
 * @param bookingId     the booking this payment settles
 * @param status        outcome; see PaymentStatus
 * @param amount        amount processed
 * @param processedAt   server timestamp of the outcome
 */
public record PaymentResponseDTO(
        String transactionId,
        String bookingId,
        PaymentStatus status,
        BigDecimal amount,
        Instant processedAt
)
{
    /**
     * Allowed payment outcomes. An enum (not a free-text String) makes the set of
     * states a compile-time contract — callers can exhaustively switch on it and
     * can't invent a typo'd status like "SUCESS".
     */
    public enum PaymentStatus
    {
        SUCCESS,
        FAILED,
        PENDING
    }
}