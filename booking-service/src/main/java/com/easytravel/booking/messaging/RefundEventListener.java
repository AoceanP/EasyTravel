package com.easytravel.booking.messaging;

import com.easytravel.booking.service.BookingService;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.RefundEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * WHAT:   Receives refund results from RabbitMQ and finishes cancellations.
 * WHY:    The last step of the cancellation saga, the mirror of
 *         PaymentEventListener: payment-service does not know booking-service
 *         exists, it only announces the result.
 *
 * @author AoceanP
 */
@Component
public class RefundEventListener
{
    /** WHAT: does the actual work. */
    private final BookingService bookingService;

    /**
     * WHAT:   Creates the listener.
     * PRE:    bookingService is not null
     *
     * @param bookingService the booking logic
     */
    public RefundEventListener(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    /**
     * WHAT:   Handles one refund result.
     * WHY:    A result for an unknown booking can never succeed: it is
     *         rejected without requeue and, after the listener's retries, waits
     *         in the dead-letter queue for a person.
     * PRE:    called by Spring for each message on the refund-results queue
     * POST:   the booking is CANCELLED or back to CONFIRMED, or the message is dead-lettered
     * THROWS: AmqpRejectAndDontRequeueException for an unknown booking
     *
     * @param event the refund result
     */
    @RabbitListener(queues = MessagingConstants.REFUND_QUEUE)
    public void onRefundEvent(RefundEvent event)
    {
        try
        {
            bookingService.applyRefundResult(event);
        }
        catch (ResourceNotFoundException e)
        {
            throw new AmqpRejectAndDontRequeueException(e.getMessage(), e);
        }
    }
}
