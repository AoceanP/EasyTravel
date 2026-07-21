package com.easytravel.common.event;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * The message payload published when a payment completes. Travels over RabbitMQ from
 * payment-service (producer) to booking-service (consumer). Lives in common so both
 * sides share one definition.
 */
public record PaymentEvent(
        String bookingId,
        String transactionId,
        boolean success,
        BigDecimal amount
) implements Serializable {
}