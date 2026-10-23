package com.easytravel.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * WHAT:   The outcome of a payment, as the payment service returns it.
 * WHY:    The same information travels to the booking service as a
 *         PaymentEvent; this is the read-only HTTP view of it.
 *
 * @param transactionId unique id of the payment attempt (what a customer would quote)
 * @param bookingId     the booking this payment settles
 * @param status        the outcome
 * @param amount        the amount charged
 * @param reason        why it failed, or null when it succeeded
 * @param processedAt   when the payment service processed it
 * @author AoceanP
 */
public record PaymentResponseDTO(
        String transactionId,
        String bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String reason,
        Instant processedAt
)
{
    /**
     * WHAT:   The possible payment outcomes.
     * WHY:    An enum, not free text, so nobody can store a typo like "SUCESS",
     *         and code can switch over every case.
     */
    public enum PaymentStatus
    {
        /** WHAT: the card was charged. */
        SUCCESS,

        /** WHAT: the charge was declined. */
        FAILED
    }
}
