package com.easytravel.payment.messaging;

import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.PaymentEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * WHAT:   Sends PaymentEvents to RabbitMQ.
 * WHY:    Runs only AFTER the payment is committed to the database, so
 *         booking-service never confirms a booking whose payment record
 *         was rolled back.
 *
 * @author AoceanP
 */
@Component
public class PaymentEventPublisher
{
    /** WHAT: Spring's RabbitMQ sender, set up with the JSON converter. */
    private final RabbitTemplate rabbitTemplate;

    /**
     * WHAT:   Creates the publisher.
     * PRE:    rabbitTemplate is not null
     *
     * @param rabbitTemplate the RabbitMQ sender
     */
    public PaymentEventPublisher(RabbitTemplate rabbitTemplate)
    {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * WHAT:   Publishes the result to "payment.exchange" with key "payment.completed".
     * PRE:    the payment's transaction committed
     * POST:   the message is with RabbitMQ
     *
     * @param event the payment result
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProcessed(PaymentEvent event)
    {
        rabbitTemplate.convertAndSend(MessagingConstants.PAYMENT_EXCHANGE, MessagingConstants.PAYMENT_ROUTING_KEY, event);
    }
}
