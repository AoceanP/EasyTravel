package com.easytravel.common.event;

/**
 * WHAT:   Every RabbitMQ exchange, queue and routing-key name, in one place.
 * WHY:    Producer and consumer must use identical strings, or messages are
 *         routed into the void with no error. Sharing the constants makes a
 *         mismatch a compile error instead of a silent bug.
 *
 *         booking-service --booking.created--> [booking.exchange] --> payment.booking-created.queue --> payment-service
 *         payment-service --payment.completed--> [payment.exchange] --> booking.payment-completed.queue --> booking-service
 *
 *         Messages that cannot be processed go to the dead-letter exchange
 *         and wait in a ".dlq" queue for a person to look at them, instead
 *         of being retried forever.
 *
 * @author AoceanP
 */
public final class MessagingConstants
{
    /** WHAT: where booking-service announces new bookings. */
    public static final String BOOKING_EXCHANGE = "booking.exchange";

    /** WHAT: the routing key for "a booking was created and needs payment". */
    public static final String BOOKING_CREATED_ROUTING_KEY = "booking.created";

    /** WHAT: payment-service's queue of bookings to charge. */
    public static final String BOOKING_CREATED_QUEUE = "payment.booking-created.queue";

    /** WHAT: where payment-service announces payment results. */
    public static final String PAYMENT_EXCHANGE = "payment.exchange";

    /** WHAT: the routing key for "a payment succeeded or failed". */
    public static final String PAYMENT_ROUTING_KEY = "payment.completed";

    /** WHAT: booking-service's queue of payment results. */
    public static final String PAYMENT_QUEUE = "booking.payment-completed.queue";

    /** WHAT: the dead-letter exchange: rejected messages are re-routed here. */
    public static final String DEAD_LETTER_EXCHANGE = "easytravel.dlx";

    /** WHAT: payment-service's dead-letter queue. */
    public static final String BOOKING_CREATED_DLQ = BOOKING_CREATED_QUEUE + ".dlq";

    /** WHAT: booking-service's dead-letter queue. */
    public static final String PAYMENT_DLQ = PAYMENT_QUEUE + ".dlq";

    // ---- the cancellation saga ----------------------------------------------
    //
    // booking-service --booking.refund-requested--> [booking.exchange] --> payment.refund-requested.queue --> payment-service
    // payment-service --payment.refunded--> [payment.exchange] --> booking.refund-completed.queue --> booking-service

    /** WHAT: the routing key for "a customer cancelled; refund this booking". */
    public static final String REFUND_REQUESTED_ROUTING_KEY = "booking.refund-requested";

    /** WHAT: payment-service's queue of refunds to make. */
    public static final String REFUND_REQUESTED_QUEUE = "payment.refund-requested.queue";

    /** WHAT: its dead-letter queue. */
    public static final String REFUND_REQUESTED_DLQ = REFUND_REQUESTED_QUEUE + ".dlq";

    /** WHAT: the routing key for "a refund succeeded or failed". */
    public static final String REFUND_ROUTING_KEY = "payment.refunded";

    /** WHAT: booking-service's queue of refund results. */
    public static final String REFUND_QUEUE = "booking.refund-completed.queue";

    /** WHAT: its dead-letter queue. */
    public static final String REFUND_DLQ = REFUND_QUEUE + ".dlq";

    /**
     * WHAT:   Blocks creating a MessagingConstants.
     * WHY:    It only holds constants.
     */
    private MessagingConstants()
    {
    }
}
