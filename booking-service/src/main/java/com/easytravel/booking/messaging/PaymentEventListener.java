package com.easytravel.booking.messaging;

import com.easytravel.booking.service.BookingService;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * WHAT:   Receives payment results from RabbitMQ and applies them to bookings.
 * WHY:    booking-service reacts to payment-service without either calling
 *         the other: payment-service does not even know booking-service
 *         exists. That decoupling is the point of event-driven design.
 *
 * @author AoceanP
 */
@Component
public class PaymentEventListener
{
    /** WHAT: does the actual work. */
    private final BookingService bookingService;

    /**
     * WHAT:   Creates the listener.
     * PRE:    bookingService is not null
     *
     * @param bookingService the booking logic
     */
    public PaymentEventListener(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    /**
     * WHAT:   Handles one payment result.
     * WHY:    A result for a booking that does not exist will never succeed,
     *         so it is rejected with "don't requeue": it must never loop back
     *         onto the queue. Spring's listener retry (application.yml) still
     *         makes its 3 attempts first, about 1.5 seconds in all, and then
     *         the message goes to the dead-letter queue. Other errors (e.g. the
     *         database is briefly down) take the same path, and those retries
     *         are what give them a chance to succeed.
     * PRE:    called by Spring for each message on the payment-results queue
     * POST:   the booking is updated, or the message is dead-lettered
     * THROWS: AmqpRejectAndDontRequeueException for an unknown booking
     *
     * @param event the payment result
     */
    @RabbitListener(queues = MessagingConstants.PAYMENT_QUEUE)
    public void onPaymentEvent(PaymentEvent event)
    {
        try
        {
            bookingService.applyPaymentResult(event);
        }
        catch (ResourceNotFoundException e)
        {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
