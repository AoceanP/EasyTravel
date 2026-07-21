package com.easytravel.common.event;

/**
 * Single source of truth for RabbitMQ topology names, shared by producer and consumer.
 * If these strings didn't match on both sides, messages would route into the void.
 */
public final class MessagingConstants {

    private MessagingConstants() {
        // utility class — never instantiated
    }

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.completed";
}