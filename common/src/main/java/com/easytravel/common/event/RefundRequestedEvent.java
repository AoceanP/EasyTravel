package com.easytravel.common.event;

import java.math.BigDecimal;

/**
 * WHAT:   "A customer cancelled a paid booking; refund it." Sent by
 *         booking-service, consumed by payment-service.
 * WHY:    Starts the cancellation saga. It carries the transaction to refund
 *         and the amount, so payment-service needs nothing else.
 *
 * @param bookingId     the booking being cancelled
 * @param transactionId the payment to refund
 * @param amount        the amount that was charged
 * @author AoceanP
 */
public record RefundRequestedEvent(String bookingId, String transactionId, BigDecimal amount)
{
}
