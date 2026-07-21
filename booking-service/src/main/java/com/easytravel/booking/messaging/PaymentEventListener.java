package com.easytravel.booking.messaging;

import com.easytravel.booking.service.BookingService;
import com.easytravel.common.event.MessagingConstants;
import com.easytravel.common.event.PaymentEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for PaymentEvents and confirms the corresponding booking.
 *
 * <p>@RabbitListener(queues = PAYMENT_QUEUE) subscribes this method to the queue. When a
 * message arrives, Spring deserializes it (via our JSON converter) into a PaymentEvent
 * and invokes this method. booking-service reacts to the event WITHOUT payment-service
 * ever calling it directly — that's the decoupling. Payment doesn't know booking exists.
 */
@Component
public class PaymentEventListener {

    private final BookingService bookingService;

    public PaymentEventListener(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @RabbitListener(queues = MessagingConstants.PAYMENT_QUEUE)
    public void onPaymentEvent(PaymentEvent event) {
        System.out.println("[booking] Received PaymentEvent for booking "
                + event.bookingId() + " (success=" + event.success() + ")");
        if (event.success()) {
            bookingService.confirmBooking(event.bookingId());
            System.out.println("[booking] Booking " + event.bookingId() + " -> CONFIRMED");
        }
    }
}