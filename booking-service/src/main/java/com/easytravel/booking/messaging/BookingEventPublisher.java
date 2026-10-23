package com.easytravel.booking.messaging;

import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.RefundRequestedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * WHAT:   Sends BookingCreatedEvents to RabbitMQ.
 * WHY:    @TransactionalEventListener(AFTER_COMMIT) runs this only once the
 *         booking's database transaction has committed. Publishing inside
 *         the transaction could announce a booking that is then rolled back,
 *         and payment-service would charge for a booking that does not exist.
 *         (A crash between the commit and the publish could still lose the
 *         message; the "transactional outbox" pattern closes that gap and is
 *         on the roadmap.)
 *
 * @author AoceanP
 */
@Component
public class BookingEventPublisher
{
    /** WHAT: Spring's RabbitMQ sender, already set up with the JSON converter. */
    private final RabbitTemplate rabbitTemplate;

    /**
     * WHAT:   Creates the publisher.
     * PRE:    rabbitTemplate is not null
     *
     * @param rabbitTemplate the RabbitMQ sender
     */
    public BookingEventPublisher(RabbitTemplate rabbitTemplate)
    {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * WHAT:   Publishes the event to "booking.exchange" with key "booking.created".
     * PRE:    the booking's transaction committed
     * POST:   the message is with RabbitMQ
     *
     * @param event the new booking
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event)
    {
        rabbitTemplate.convertAndSend(MessagingConstants.BOOKING_EXCHANGE,
                MessagingConstants.BOOKING_CREATED_ROUTING_KEY, event);
    }

    /**
     * WHAT:   Publishes a refund request to "booking.exchange" with key "booking.refund-requested".
     * WHY:    After commit for the same reason: no refund for a cancellation that rolled back.
     * PRE:    the cancellation's transaction committed
     * POST:   the message is with RabbitMQ
     *
     * @param event the refund request
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundRequested(RefundRequestedEvent event)
    {
        rabbitTemplate.convertAndSend(MessagingConstants.BOOKING_EXCHANGE,
                MessagingConstants.REFUND_REQUESTED_ROUTING_KEY, event);
    }
}
