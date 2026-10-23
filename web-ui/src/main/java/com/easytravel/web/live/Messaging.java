package com.easytravel.web.live;

/**
 * WHAT:   The RabbitMQ names the website listens on.
 * WHY:    Same values as MessagingConstants in the common module, copied so this
 *         WAR stays free of Spring. They are the contract between the services:
 *         changing one side means changing the other.
 *
 * @author AoceanP
 */
public final class Messaging
{
    /** WHAT: where booking-service announces new bookings. */
    public static final String BOOKING_EXCHANGE = "booking.exchange";
    public static final String BOOKING_CREATED_KEY = "booking.created";

    /** WHAT: where payment-service announces payment results. */
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_COMPLETED_KEY = "payment.completed";

    /** WHAT: the cancellation saga: a refund is requested, then reported. */
    public static final String REFUND_REQUESTED_KEY = "booking.refund-requested";
    public static final String REFUNDED_KEY = "payment.refunded";

    /** WHAT: the Faces push channel that tells a browser its booking changed. */
    public static final String PUSH_CHANNEL = "booking";

    /** WHAT: the Faces push channel every open Ops room listens on. */
    public static final String OPS_CHANNEL = "ops";
    public static final String PUSH_UPDATED = "updated";

    /**
     * WHAT:   Not instantiable.
     * WHY:    Constants only.
     */
    private Messaging()
    {
    }
}
