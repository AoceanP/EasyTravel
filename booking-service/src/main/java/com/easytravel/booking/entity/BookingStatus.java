package com.easytravel.booking.entity;

/**
 * WHAT:   Where a booking is in its life.
 * WHY:    An enum, not free text, makes the valid states a compile-time
 *         contract: nobody can store a typo like "PENDNIG".
 *
 *             PENDING ----payment succeeded----> CONFIRMED
 *             PENDING ----payment declined-----> PAYMENT_FAILED   (final)
 *             CONFIRMED --customer cancels----> CANCELLING
 *             CANCELLING --refund succeeded---> CANCELLED         (final)
 *             CANCELLING --refund failed------> CONFIRMED         (the ticket stays valid)
 *
 *         Every move is checked in Booking, so a late or duplicated event
 *         can never take a booking down a path it is not on.
 *
 * @author AoceanP
 */
public enum BookingStatus
{
    /** WHAT: created, waiting for the payment result. */
    PENDING,

    /** WHAT: paid; the seat is the customer's. */
    CONFIRMED,

    /** WHAT: the payment was declined; the booking will not happen. */
    PAYMENT_FAILED,

    /** WHAT: the customer cancelled; waiting for the refund. */
    CANCELLING,

    /** WHAT: cancelled and refunded. */
    CANCELLED
}
