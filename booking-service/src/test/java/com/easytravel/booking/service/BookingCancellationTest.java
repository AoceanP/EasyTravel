package com.easytravel.booking.service;

import com.easytravel.booking.client.InventoryClient;
import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.BookingStatus;
import com.easytravel.booking.repository.BookingRepository;
import com.easytravel.common.dto.FareClass;
import com.easytravel.common.event.RefundEvent;
import com.easytravel.common.event.RefundRequestedEvent;
import com.easytravel.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WHAT:   Tests for the cancellation saga's booking side: cancelling, and
 *         applying refund results.
 * WHY:    Money goes back to a card here, so every status, duplicate and
 *         failure path is checked.
 *
 * @author AoceanP
 */
class BookingCancellationTest
{
    /** WHAT: a fixed time. */
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    /** WHAT: the id of the test booking. */
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    /** WHAT: the fake database. */
    private BookingRepository repository;

    /** WHAT: the fake event bus. */
    private ApplicationEventPublisher events;

    /** WHAT: the service under test. */
    private BookingService service;

    /**
     * WHAT:   Builds the service with fakes.
     * POST:   service is ready
     */
    @BeforeEach
    void setUp()
    {
        repository = mock(BookingRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new BookingService(repository, mock(InventoryClient.class), events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * WHAT:   Cancelling a paid booking.
     * WHY:    It becomes CANCELLING and a refund of the agreed price is requested.
     * RETURN: nothing; fails if the status or event is wrong
     */
    @Test
    void cancellingAPaidBookingRequestsARefund()
    {
        Booking booking = stored(BookingStatus.CONFIRMED);

        assertThat(service.cancelBooking(ID).getStatus()).isEqualTo(BookingStatus.CANCELLING);

        ArgumentCaptor<RefundRequestedEvent> event = ArgumentCaptor.forClass(RefundRequestedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new RefundRequestedEvent(ID.toString(), "tx-1", booking.getAgreedPrice()));
    }

    /**
     * WHAT:   Cancelling twice (a double click, or a retried request).
     * WHY:    The second call changes nothing and requests no second refund.
     * RETURN: nothing; fails if two refunds are requested
     */
    @Test
    void cancellingTwiceRequestsOneRefund()
    {
        stored(BookingStatus.CONFIRMED);
        service.cancelBooking(ID);
        service.cancelBooking(ID);
        verify(events, times(1)).publishEvent(any(Object.class));
    }

    /**
     * WHAT:   Cancelling a booking that is still being paid, or was never paid.
     * WHY:    Both are 409 Conflict with a reason, and nothing is requested.
     * RETURN: nothing; fails unless both are refused
     */
    @Test
    void unpaidBookingsCannotBeCancelled()
    {
        stored(BookingStatus.PENDING);
        assertThatThrownBy(() -> service.cancelBooking(ID)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("still waiting for payment");

        stored(BookingStatus.PAYMENT_FAILED);
        assertThatThrownBy(() -> service.cancelBooking(ID)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("never paid");
        verify(events, never()).publishEvent(any(Object.class));
    }

    /**
     * WHAT:   A successful refund result, then the same result again.
     * WHY:    The booking ends CANCELLED; the duplicate is ignored.
     * RETURN: nothing; fails if the status is wrong or the duplicate counts
     */
    @Test
    void refundSucceededCancels()
    {
        Booking booking = stored(BookingStatus.CANCELLING);
        RefundEvent ok = new RefundEvent(ID.toString(), "tx-1", true, booking.getAgreedPrice(), null);

        assertThat(service.applyRefundResult(ok)).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(service.applyRefundResult(ok)).isFalse();
    }

    /**
     * WHAT:   A failed refund.
     * WHY:    The customer still paid, so the ticket goes back to CONFIRMED,
     *         with the reason noted.
     * RETURN: nothing; fails if the booking is not restored
     */
    @Test
    void refundFailedRestoresTheTicket()
    {
        Booking booking = stored(BookingStatus.CANCELLING);
        service.applyRefundResult(new RefundEvent(ID.toString(), "tx-1", false, booking.getAgreedPrice(), "card closed"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getFailureReason()).isEqualTo("refund failed: card closed");
    }

    /**
     * WHAT:   A refund result for a booking that is not being cancelled.
     * WHY:    It must never cancel a confirmed booking on its own.
     * RETURN: nothing; fails if the booking changes
     */
    @Test
    void strayRefundResultIsIgnored()
    {
        Booking booking = stored(BookingStatus.CONFIRMED);
        assertThat(service.applyRefundResult(new RefundEvent(ID.toString(), "tx-1", true, BigDecimal.TEN, null))).isFalse();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    /**
     * WHAT:   A booking with id ID in the given status, returned by the fake repository.
     * PRE:    status is reachable through the entity's own methods
     * RETURN: the booking
     *
     * @param status the status to put it in
     * @return the booking
     */
    private Booking stored(BookingStatus status)
    {
        Booking b = new Booking("alice", "BA249", "LHR", "JFK", new BigDecimal("210.00"), FareClass.ECONOMY, NOW);
        ReflectionTestUtils.setField(b, "id", ID);
        switch (status)
        {
            case PENDING -> { }
            case PAYMENT_FAILED -> b.markPaymentFailed("tx-1", "limit", NOW);
            case CONFIRMED -> b.confirm("tx-1", NOW);
            case CANCELLING -> { b.confirm("tx-1", NOW); b.requestCancel(NOW); }
            case CANCELLED -> { b.confirm("tx-1", NOW); b.requestCancel(NOW); b.markCancelled(NOW); }
        }
        when(repository.findById(ID)).thenReturn(Optional.of(b));
        return b;
    }
}
