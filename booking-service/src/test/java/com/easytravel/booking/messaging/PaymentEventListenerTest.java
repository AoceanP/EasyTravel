package com.easytravel.booking.messaging;

import com.easytravel.booking.service.BookingService;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WHAT:   Tests for the RabbitMQ listener's error handling.
 * WHY:    A message for a booking that does not exist must go to the
 *         dead-letter queue, not loop forever.
 *
 * @author AoceanP
 */
class PaymentEventListenerTest
{
    /**
     * WHAT:   A normal payment result.
     * WHY:    Passed straight to the service.
     * PRE:    none
     * RETURN: nothing; fails if the service is not called
     */
    @Test
    void passesResultToService()
    {
        BookingService service = mock(BookingService.class);
        PaymentEvent event = new PaymentEvent("7", "tx", true, BigDecimal.TEN, null);
        new PaymentEventListener(service).onPaymentEvent(event);
        verify(service).applyPaymentResult(event);
    }

    /**
     * WHAT:   A result for an unknown booking.
     * WHY:    Rejected without requeue: RabbitMQ then dead-letters it.
     * PRE:    none
     * RETURN: nothing; fails unless AmqpRejectAndDontRequeueException is thrown
     */
    @Test
    void unknownBookingIsDeadLettered()
    {
        BookingService service = mock(BookingService.class);
        PaymentEvent event = new PaymentEvent("99", "tx", true, BigDecimal.TEN, null);
        when(service.applyPaymentResult(event)).thenThrow(new ResourceNotFoundException("booking 99 not found"));

        assertThatThrownBy(() -> new PaymentEventListener(service).onPaymentEvent(event))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }
}
