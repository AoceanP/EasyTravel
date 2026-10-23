package com.easytravel.web.trips;

import com.easytravel.web.book.BookingProgress.Step;
import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.live.BookingTracker.Track;

import java.util.List;

/**
 * WHAT:   The four steps of a cancellation and which have happened:
 *         Cancellation requested → Refund requested → Refund sent/failed → Trip cancelled/Ticket kept.
 * WHY:    Like BookingProgress, each step lights only on evidence: the
 *         booking's status from booking-service, or a refund event the website
 *         heard on RabbitMQ.
 *
 * @param steps the four steps
 * @param done  whether booking-service has finished the cancellation (either way)
 * @param ok    whether it ended CANCELLED (the refund went through)
 * @author AoceanP
 */
public record CancelProgress(List<Step> steps, boolean done, boolean ok)
{
    /** WHAT: the prefix booking-service puts on failureReason when a refund fails. */
    static final String REFUND_FAILED = "refund failed";

    /**
     * WHAT:   Works out the progress of a cancellation.
     * PRE:    booking is not null
     * RETURN: the progress
     *
     * @param booking the booking as last read
     * @param track   the events heard for it, or null
     * @return the progress
     */
    public static CancelProgress of(BookingDto booking, Track track)
    {
        boolean refundFailed = "CONFIRMED".equals(booking.status()) && booking.failureReason() != null
                && booking.failureReason().startsWith(REFUND_FAILED);
        boolean cancelled = "CANCELLED".equals(booking.status());
        boolean done = cancelled || refundFailed;
        boolean requested = done || "CANCELLING".equals(booking.status());
        boolean sent = done || (track != null && track.refundRequested() != null);
        boolean heard = track != null && track.refunded() != null;
        boolean answered = done || heard;
        boolean ok = done ? cancelled : !heard || track.refundOk();

        String result = ok ? "good" : "bad";
        String[] css = {
                requested ? "on" : "idle",
                sent ? "on" : "idle",
                answered ? result : "idle",
                done ? result : "idle"
        };
        List<Step> steps = List.of(
                new Step("Cancellation requested", "booking-service · CANCELLING", "step " + css[0], "line " + css[1], false),
                new Step("Refund requested", "RefundRequestedEvent → payment", "step " + css[1], "line " + css[2], false),
                new Step(answered && !ok ? "Refund failed" : "Refund sent to your card", "payment-service · mock card",
                        "step " + css[2], "line " + css[3], false),
                new Step(done && !ok ? "Ticket kept" : "Trip cancelled",
                        done && !ok ? "RefundEvent → CONFIRMED" : "RefundEvent → CANCELLED", "step " + css[3], "", true));
        return new CancelProgress(steps, done, ok);
    }
}
