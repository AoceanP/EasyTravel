package com.easytravel.web.trips;

import com.easytravel.web.book.BookingProgress.Step;
import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.live.BookingTracker.Track;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for the cancellation steps and the boarding-pass barcode.
 *
 * @author AoceanP
 */
class CancelProgressTest
{
    /** WHAT: a fixed time. */
    private static final Instant T = Instant.parse("2026-10-23T10:00:00Z");

    /**
     * WHAT:   Just cancelled; the refund request has been heard.
     * WHY:    The first two steps are lit; the outcome waits.
     * RETURN: nothing; fails if the steps are wrong
     */
    @Test
    void requestedAndSent()
    {
        CancelProgress p = CancelProgress.of(booking("CANCELLING", null), track(T, null, false, null));
        assertThat(p.steps()).extracting(Step::css).containsExactly("step on", "step on", "step idle", "step idle");
        assertThat(p.done()).isFalse();
    }

    /**
     * WHAT:   Refunded and cancelled, even with no events heard.
     * WHY:    booking-service's status alone completes every step.
     * RETURN: nothing; fails unless all steps are green
     */
    @Test
    void cancelledFromStatusAlone()
    {
        CancelProgress p = CancelProgress.of(booking("CANCELLED", null), null);
        assertThat(p.steps()).extracting(Step::css).containsExactly("step on", "step on", "step good", "step good");
        assertThat(p.ok()).isTrue();
    }

    /**
     * WHAT:   The refund failed and the ticket was restored.
     * WHY:    The last two steps turn red and say what happened.
     * RETURN: nothing; fails if the labels are wrong
     */
    @Test
    void refundFailedKeepsTheTicket()
    {
        CancelProgress p = CancelProgress.of(booking("CONFIRMED", "refund failed: card closed"),
                track(T, T, false, "card closed"));
        assertThat(p.steps()).extracting(Step::label)
                .containsExactly("Cancellation requested", "Refund requested", "Refund failed", "Ticket kept");
        assertThat(p.done()).isTrue();
        assertThat(p.ok()).isFalse();
    }

    /**
     * WHAT:   The barcode for a booking id.
     * WHY:    Stable for the same id, starts and ends with a dark bar.
     * RETURN: nothing; fails if it changes or has the wrong shape
     */
    @Test
    void barcodeIsStable()
    {
        String id = "294c1e96-8e67-41a0-be8e-a80378c90fa7";
        assertThat(Barcode.of(id)).isEqualTo(Barcode.of(id)).hasSize(Barcode.ELEMENTS);
        assertThat(Barcode.of(id).getFirst().css()).isEqualTo("bar-dark");
        assertThat(Barcode.of(id).getLast().css()).isEqualTo("bar-dark");
    }

    /**
     * WHAT:   A booking in a status.
     * PRE:    none
     * RETURN: the booking
     *
     * @param status the status
     * @param reason its failure reason, or null
     * @return the booking
     */
    private static BookingDto booking(String status, String reason)
    {
        return new BookingDto("b-1", "alice", "BA249", "LHR", "JFK", "ECONOMY", new BigDecimal("210.00"),
                status, "tx-1", reason, T, T);
    }

    /**
     * WHAT:   A track with refund events.
     * PRE:    none
     * RETURN: the track
     *
     * @param requested when the refund request was heard, or null
     * @param refunded  when the result was heard, or null
     * @param ok        the result
     * @param reason    why it failed, or null
     * @return the track
     */
    private static Track track(Instant requested, Instant refunded, boolean ok, String reason)
    {
        return new Track("w", T, T, true, null, requested, refunded, ok, reason, T);
    }
}
