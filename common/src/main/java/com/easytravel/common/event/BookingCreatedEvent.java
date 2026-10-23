package com.easytravel.common.event;

import java.math.BigDecimal;

/**
 * WHAT:   The message booking-service publishes when a booking is created.
 * WHY:    It starts the payment step of the saga. The amount is the price
 *         booking-service computed from the real fare, so payment-service
 *         never sees a price chosen by the client.
 *
 * @param bookingId the new booking's id
 * @param userId    the customer to charge
 * @param amount    the price to charge
 * @author AoceanP
 */
public record BookingCreatedEvent(
        String bookingId,
        String userId,
        BigDecimal amount
)
{
}
