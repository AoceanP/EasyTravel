package com.easytravel.booking.entity;

/**
 * WHAT:   Where a booking is in its life.
 * WHY:    An enum, not free text, makes the valid states a compile-time
 *         contract: nobody can store a typo like "PENDNIG".
 *
 *             PENDING --payment succeeded--> CONFIRMED
 *             PENDING --payment declined---> PAYMENT_FAILED
 *
 *         Only PENDING can change; the other two are final.
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
    PAYMENT_FAILED
}
