package com.easytravel.web.book;

import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.live.BookingTracker.Track;

import java.util.List;

/**
 * WHAT:   The four steps on the ticket and which of them have happened:
 *         Booking saved → Payment sent → Charged/Declined → Confirmed/Not booked.
 * WHY:    Each step lights up only on real evidence: the booking returned by the
 *         gateway, or an event the website heard on RabbitMQ. Nothing is timed
 *         or guessed, so the page never shows a step the system has not reached.
 *
 * @param steps     the four steps, in order
 * @param decided   whether booking-service has a final status
 * @param confirmed whether that status is CONFIRMED
 * @author AoceanP
 */
public record BookingProgress(List<Step> steps, boolean decided, boolean confirmed)
{
    /**
     * WHAT:   One step.
     *
     * @param label what a customer reads
     * @param tech  what happens behind the scenes (service, event)
     * @param css   CSS classes for its dot and label
     * @param line  CSS class for the line to the next step
     * @param last  whether it is the last step (no line after it)
     */
    public record Step(String label, String tech, String css, String line, boolean last)
    {
    }

    /**
     * WHAT:   Works out the progress of a booking.
     * PRE:    none (both arguments may be null)
     * RETURN: the progress; all steps idle when there is no booking
     *
     * @param booking the booking as last read from the gateway, or null
     * @param track   the events heard for it, or null
     * @return the progress
     */
    public static BookingProgress of(BookingDto booking, Track track)
    {
        boolean saved = booking != null;
        boolean decided = saved && booking.isDecided();
        boolean confirmed = decided && "CONFIRMED".equals(booking.status());
        boolean paidSeen = track != null && track.paid() != null;
        boolean sent = decided || paidSeen || (track != null && track.created() != null);
        boolean charged = decided || paidSeen;
        // until booking-service has decided, the event tells which way it will go
        boolean ok = decided ? confirmed : !paidSeen || track.success();

        String result = ok ? "good" : "bad";
        String[] css = {
                saved ? "on" : "idle",
                sent ? "on" : "idle",
                charged ? result : "idle",
                decided ? result : "idle"
        };
        List<Step> steps = List.of(
                new Step("Booking saved", "booking-service · PENDING", "step " + css[0], "line " + css[1], false),
                new Step("Payment sent", "BookingCreatedEvent → payment", "step " + css[1], "line " + css[2], false),
                new Step(charged && !ok ? "Declined" : "Charged", "payment-service · mock card", "step " + css[2], "line " + css[3], false),
                new Step(decided && !ok ? "Not booked" : "Confirmed",
                        decided && !ok ? "PaymentEvent → PAYMENT_FAILED" : "PaymentEvent → CONFIRMED",
                        "step " + css[3], "", true));
        return new BookingProgress(steps, decided, confirmed);
    }
}
