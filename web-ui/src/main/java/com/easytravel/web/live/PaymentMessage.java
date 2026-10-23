package com.easytravel.web.live;

import java.math.BigDecimal;

/**
 * WHAT:   The PaymentEvent JSON, as the website reads it from RabbitMQ.
 * WHY:    Its arrival means payment has charged or declined that booking.
 *
 * @param bookingId     the booking
 * @param transactionId the payment's id
 * @param success       whether the card was charged
 * @param amount        the amount
 * @param reason        why it was declined, or null
 * @author AoceanP
 */
public record PaymentMessage(String bookingId, String transactionId, boolean success, BigDecimal amount, String reason)
{
}
