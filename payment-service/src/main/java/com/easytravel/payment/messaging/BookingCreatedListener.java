package com.easytravel.payment.messaging;

import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.payment.service.PaymentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * WHAT:   Receives new bookings from RabbitMQ and charges them.
 * WHY:    Payments now start only from this event, with the price
 *         booking-service computed. There is no public "pay any amount"
 *         endpoint any more, so nobody can pay a booking with a price of
 *         their own choosing.
 *
 * @author AoceanP
 */
@Component
public class BookingCreatedListener
{
    /** WHAT: does the actual work. */
    private final PaymentService paymentService;

    /**
     * WHAT:   Creates the listener.
     * PRE:    paymentService is not null
     *
     * @param paymentService the payment logic
     */
    public BookingCreatedListener(PaymentService paymentService)
    {
        this.paymentService = paymentService;
    }

    /**
     * WHAT:   Handles one new booking.
     * WHY:    If a duplicate slipped past the "already paid?" check at the
     *         same moment as the original, the database's unique constraint
     *         refuses it; that is a duplicate, not an error, so it is dropped.
     * PRE:    called by Spring for each message on the booking-created queue
     * POST:   the booking is charged once, however many times the message arrives
     *
     * @param event the new booking
     */
    @RabbitListener(queues = MessagingConstants.BOOKING_CREATED_QUEUE)
    public void onBookingCreated(BookingCreatedEvent event)
    {
        try
        {
            paymentService.processBooking(event);
        }
        catch (DataIntegrityViolationException duplicate)
        {
            // another delivery of the same booking won the race: nothing left to do
        }
    }
}
