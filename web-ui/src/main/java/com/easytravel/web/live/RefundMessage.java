package com.easytravel.web.live;

import java.math.BigDecimal;

/**
 * WHAT:   The RefundRequestedEvent and RefundEvent JSON, read with one record.
 * WHY:    The website only needs the booking id from a request, and the
 *         outcome from a result; fields a message lacks are simply absent.
 *
 * @param bookingId     the booking
 * @param transactionId the payment being refunded
 * @param success       whether the refund went through (results only)
 * @param amount        the amount
 * @param reason        why it failed, or null
 * @author AoceanP
 */
public record RefundMessage(String bookingId, String transactionId, boolean success, BigDecimal amount, String reason)
{
}
