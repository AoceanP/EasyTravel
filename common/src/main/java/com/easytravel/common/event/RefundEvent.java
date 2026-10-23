package com.easytravel.common.event;

import java.math.BigDecimal;

/**
 * WHAT:   The result of a refund. Sent by payment-service, consumed by
 *         booking-service.
 * WHY:    Like PaymentEvent, it reports failure as well as success, so a
 *         cancellation can never be left hanging in CANCELLING.
 *
 * @param bookingId     the booking
 * @param transactionId the payment that was (or was not) refunded
 * @param success       whether the money went back to the card
 * @param amount        the amount
 * @param reason        why it failed, or null
 * @author AoceanP
 */
public record RefundEvent(String bookingId, String transactionId, boolean success, BigDecimal amount, String reason)
{
}
