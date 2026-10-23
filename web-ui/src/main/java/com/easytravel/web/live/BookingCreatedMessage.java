package com.easytravel.web.live;

import java.math.BigDecimal;

/**
 * WHAT:   The BookingCreatedEvent JSON, as the website reads it from RabbitMQ.
 * WHY:    Its arrival means "payment has been requested" for that booking.
 *
 * @param bookingId the booking
 * @param userId    who booked
 * @param amount    what payment will charge
 * @author AoceanP
 */
public record BookingCreatedMessage(String bookingId, String userId, BigDecimal amount)
{
}
