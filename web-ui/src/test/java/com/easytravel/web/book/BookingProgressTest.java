package com.easytravel.web.book;

import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.live.BookingTracker.Track;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the ticket's four steps.
 * WHY:    A step must light up only on real evidence; these cover each moment
 *         of the saga, including the race where the event beats the HTTP reply.
 *
 * @author AoceanP
 */
class BookingProgressTest
{
    /** WHAT: a fixed time. */
    private static final Instant T = Instant.parse("2026-10-23T10:00:00Z");

    /**
     * WHAT:   No booking yet.
     * WHY:    Nothing is lit and nothing is decided.
     * RETURN: nothing; fails if any step is on
     */
    @Test
    void nothingBeforeBooking()
    {
        BookingProgress p = BookingProgress.of(null, null);
        assertThat(p.steps()).extracting(BookingProgress.Step::css).containsOnly("step idle");
        assertThat(p.decided()).isFalse();
    }

    /**
     * WHAT:   Saved, and the BookingCreatedEvent has been heard.
     * WHY:    The first two steps are amber; the last two wait.
     * RETURN: nothing; fails if the steps are wrong
     */
    @Test
    void savedAndSent()
    {
        BookingProgress p = BookingProgress.of(booking("PENDING"), new Track("w", T, null, false, null, null, null, false, null, T));
        assertThat(p.steps()).extracting(BookingProgress.Step::css)
                .containsExactly("step on", "step on", "step idle", "step idle");
    }

    /**
     * WHAT:   Payment declined, and booking-service has recorded it.
     * WHY:    The last two steps turn red and are relabelled.
     * RETURN: nothing; fails if labels or colours are wrong
     */
    @Test
    void declined()
    {
        BookingProgress p = BookingProgress.of(booking("PAYMENT_FAILED"), new Track("w", T, T, false, "limit", null, null, false, null, T));
        assertThat(p.steps()).extracting(BookingProgress.Step::label)
                .containsExactly("Booking saved", "Payment sent", "Declined", "Not booked");
        assertThat(p.steps().get(3).css()).isEqualTo("step bad");
        assertThat(p.decided()).isTrue();
        assertThat(p.confirmed()).isFalse();
    }

    /**
     * WHAT:   Confirmed, but the website missed every event (RabbitMQ was down).
     * WHY:    booking-service's own status is enough to light every step.
     * RETURN: nothing; fails unless all steps are done
     */
    @Test
    void confirmedWithoutEvents()
    {
        BookingProgress p = BookingProgress.of(booking("CONFIRMED"), null);
        assertThat(p.steps()).extracting(BookingProgress.Step::css)
                .containsExactly("step on", "step on", "step good", "step good");
        assertThat(p.confirmed()).isTrue();
    }

    /**
     * WHAT:   The payment result was heard but booking-service still says PENDING.
     * WHY:    "Charged" may light, but "Confirmed" must wait for booking-service.
     * RETURN: nothing; fails if the last step lights early
     */
    @Test
    void lastStepWaitsForBookingService()
    {
        BookingProgress p = BookingProgress.of(booking("PENDING"), new Track("w", T, T, true, null, null, null, false, null, T));
        assertThat(p.steps().get(2).css()).isEqualTo("step good");
        assertThat(p.steps().get(3).css()).isEqualTo("step idle");
        assertThat(p.decided()).isFalse();
    }

    /**
     * WHAT:   A booking in a given status.
     * PRE:    none
     * RETURN: the booking
     *
     * @param status the status
     * @return the booking
     */
    private static BookingDto booking(String status)
    {
        return new BookingDto("b-1", "alice", "BA249", "LHR", "JFK", "ECONOMY", new BigDecimal("210.00"),
                status, null, null, T, T);
    }
}
