package com.easytravel.common.event;

import java.math.BigDecimal;

/**
 * WHAT:   The message payment-service publishes after charging a booking.
 * WHY:    It ends the saga: booking-service turns the booking CONFIRMED or
 *         PAYMENT_FAILED. Failures are published too; otherwise a declined
 *         booking would stay PENDING forever.
 *
 * @param bookingId     the booking that was charged
 * @param transactionId the payment's unique id
 * @param success       whether the charge went through
 * @param amount        the amount charged
 * @param reason        why it failed, or null on success
 * @author AoceanP
 */
public record PaymentEvent(
        String bookingId,
        String transactionId,
        boolean success,
        BigDecimal amount,
        String reason
)
{
}
