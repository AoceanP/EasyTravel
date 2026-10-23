package com.easytravel.web.trips;

import com.easytravel.web.book.Airlines;
import com.easytravel.web.book.BookView.Tone;
import com.easytravel.web.book.Cabin;
import com.easytravel.web.book.Money;
import com.easytravel.web.book.Visitor;
import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.gateway.GatewayException;
import com.easytravel.web.gateway.TravelService;
import com.easytravel.web.live.BookingTracker;
import jakarta.annotation.PostConstruct;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * WHAT:   The My trips page: the customer's bookings, a boarding pass for the
 *         selected one, and cancelling a trip with a live refund tracker.
 * WHY:    Cancelling asks first ("are you sure?"), because it returns money
 *         and cannot be undone. After that the page follows the cancellation
 *         saga live, the same way the Book page follows a booking.
 *
 * @author AoceanP
 */
@Named
@ViewScoped
public class TripsView implements Serializable
{
    /** WHAT: how dates are shown, e.g. "23 Oct 2026, 10:28 UTC". */
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'", Locale.UK).withZone(ZoneOffset.UTC);

    @Inject
    TravelService travel;

    @Inject
    BookingTracker tracker;

    @Inject
    Visitor visitor;

    /** WHAT: the customer's bookings, newest first. */
    private List<BookingDto> trips = List.of();

    /** WHAT: why the trips could not be loaded, or null. */
    private String loadError;

    /** WHAT: the booking whose pass is showing. */
    private String selectedId;

    /** WHAT: the booking whose "are you sure?" question is open, or null. */
    private String confirmingId;

    /** WHAT: the booking being cancelled on this page, or null. */
    private String cancellingId;

    private String message;
    private Tone tone = Tone.MUTED;
    private boolean details;

    /**
     * WHAT:   Loads the trips when the page opens.
     * PRE:    called once by CDI
     * POST:   trips are loaded and one is selected, or loadError is set
     */
    @PostConstruct
    void init()
    {
        load();
        trips.stream().filter(b -> "CONFIRMED".equals(b.status())).findFirst()
                .or(() -> trips.stream().findFirst())
                .ifPresent(b -> selectedId = b.id());
    }

    /**
     * WHAT:   (Re)loads the trips; also the "Try again" button.
     * WHY:    Bookings still moving (PENDING or CANCELLING) are watched, so
     *         their result arrives live even if they were started elsewhere.
     * PRE:    none
     * POST:   trips and loadError are updated
     */
    public void load()
    {
        try
        {
            trips = travel.bookingsFor(visitor.getUserId());
            loadError = null;
            trips.stream().filter(b -> "PENDING".equals(b.status()) || "CANCELLING".equals(b.status()))
                    .forEach(b -> tracker.watch(b.id(), visitor.getKey()));
        }
        catch (GatewayException e)
        {
            trips = List.of();
            loadError = e.getMessage();
        }
    }

    // ------------------------------------------------------------- actions

    /**
     * WHAT:   Shows a trip's boarding pass.
     * PRE:    id is one of the trips
     * POST:   it is selected; any open question is closed
     *
     * @param id the booking
     */
    public void select(String id)
    {
        selectedId = id;
        confirmingId = null;
    }

    /**
     * WHAT:   Opens "are you sure?" for a trip.
     * PRE:    id is a CONFIRMED trip
     * POST:   the question shows on that trip, which is selected
     *
     * @param id the booking
     */
    public void askCancel(String id)
    {
        selectedId = id;
        confirmingId = id;
    }

    /**
     * WHAT:   Closes the question without cancelling.
     * PRE:    none
     * POST:   no question is open
     */
    public void keep()
    {
        confirmingId = null;
    }

    /**
     * WHAT:   Cancels a trip and starts its refund.
     * PRE:    the customer confirmed
     * POST:   the trip is CANCELLING and watched, or a message says why not
     *
     * @param id the booking
     */
    public void cancel(String id)
    {
        confirmingId = null;
        try
        {
            BookingDto updated = travel.cancel(id);
            tracker.watch(id, visitor.getKey());
            replace(updated);
            cancellingId = id;
            selectedId = id;
            say("Cancelling " + updated.flightNumber() + ". Your refund of " + Money.format(updated.price())
                    + " is on its way.", Tone.MUTED);
            // the refund result may already have arrived while we waited for the response
            tracker.get(id).filter(t -> t.refunded() != null).ifPresent(t -> refresh());
        }
        catch (GatewayException e)
        {
            say(e.getMessage(), Tone.BAD);
            load();
        }
    }

    /**
     * WHAT:   Handles the live push: a watched booking changed.
     * PRE:    called by the f:ajax nested in the page's f:websocket
     * POST:   same as refresh()
     *
     * @param event the ajax event (unused)
     */
    public void onLiveUpdate(AjaxBehaviorEvent event)
    {
        refresh();
    }

    /**
     * WHAT:   Re-reads the trips, settling the cancellation in progress if its result arrived.
     * PRE:    none
     * POST:   trips and message reflect booking-service's current state
     */
    public void refresh()
    {
        try
        {
            if (cancellingId != null && tracker.get(cancellingId).map(t -> t.refunded() != null).orElse(false))
            {
                travel.awaitCancellation(cancellingId);
            }
            load();
            BookingDto c = find(cancellingId);
            if (c != null)
            {
                CancelProgress p = CancelProgress.of(c, tracker.get(c.id()).orElse(null));
                if (p.done() && p.ok())
                {
                    say("Refunded " + Money.format(c.price()) + " to your card. Your " + c.flightNumber()
                            + " trip is cancelled.", Tone.GOOD);
                }
                else if (p.done())
                {
                    say("The refund didn't go through (" + c.failureReason() + "), so your ticket is still valid.", Tone.BAD);
                }
            }
        }
        catch (GatewayException e)
        {
            say(e.getMessage(), Tone.BAD);
        }
    }

    /**
     * WHAT:   Puts an updated booking in place of the old one in the list.
     * PRE:    updated is not null
     * POST:   the list holds the new version
     *
     * @param updated the booking
     */
    private void replace(BookingDto updated)
    {
        List<BookingDto> copy = new ArrayList<>(trips);
        copy.replaceAll(b -> b.id().equals(updated.id()) ? updated : b);
        trips = List.copyOf(copy);
    }

    /**
     * WHAT:   Finds a trip by id.
     * PRE:    none
     * RETURN: the trip, or null
     *
     * @param id the booking id, or null
     * @return the trip
     */
    private BookingDto find(String id)
    {
        return id == null ? null : trips.stream().filter(b -> b.id().equals(id)).findFirst().orElse(null);
    }

    /**
     * WHAT:   Sets the status line.
     * PRE:    text is not null
     * POST:   message and tone updated
     *
     * @param text the message
     * @param t    its tone
     */
    private void say(String text, Tone t)
    {
        message = text;
        tone = t;
    }

    // ------------------------------------------------------------- what the page reads

    /**
     * WHAT:   The trip rows.
     * PRE:    none
     * RETURN: one card per booking, newest first
     *
     * @return the cards
     */
    public List<TripCard> getTrips()
    {
        List<TripCard> cards = new ArrayList<>();
        for (BookingDto b : trips)
        {
            String[] pill = pill(b);
            String note = note(b);
            boolean selected = b.id().equals(selectedId);
            cards.add(new TripCard(b.id(), b.id().substring(0, 8), b.flightNumber(), Airlines.name(b.flightNumber()),
                    Airlines.code(b.flightNumber()), "mono " + Airlines.css(b.flightNumber()), cabin(b),
                    Money.format(b.price()), b.createdAt() == null ? "" : WHEN.format(b.createdAt()),
                    pill[0], "pill " + pill[1], note,
                    "PAYMENT_FAILED".equals(b.status()) || (note != null && note.startsWith("The refund")) ? "note note-warn" : "note",
                    selected ? "trip trip-on" : "trip", selected,
                    "CONFIRMED".equals(b.status()) && !Objects.equals(cancellingId, b.id()),
                    b.id().equals(confirmingId)));
        }
        return cards;
    }

    /**
     * WHAT:   The boarding pass for the selected trip.
     * PRE:    none
     * RETURN: the pass, or null when no trip is selected
     *
     * @return the pass
     */
    public BoardingPass getPass()
    {
        BookingDto b = find(selectedId);
        if (b == null) return null;
        String stamp = switch (b.status())
        {
            case "PAYMENT_FAILED" -> "DECLINED";
            case "CANCELLING" -> "CANCELLING";
            case "CANCELLED" -> "CANCELLED";
            case "PENDING" -> "PAYING";
            default -> null;
        };
        String stampCss = switch (b.status())
        {
            case "PAYMENT_FAILED" -> "stamp stamp-bad";
            case "CANCELLED" -> "stamp stamp-muted";
            default -> "stamp stamp-warn";
        };
        return new BoardingPass(b.flightNumber(), Airlines.name(b.flightNumber()), cabin(b).toUpperCase(Locale.ROOT),
                Money.format(b.price()), b.id().substring(0, 8),
                b.createdAt() == null ? "" : WHEN.format(b.createdAt()), stamp, stampCss, Barcode.of(b.id()));
    }

    /**
     * WHAT:   The progress of the cancellation started on this page.
     * PRE:    none
     * RETURN: the progress, or null when nothing is being cancelled here
     *
     * @return the progress
     */
    public CancelProgress getCancelProgress()
    {
        BookingDto c = find(cancellingId);
        return c == null ? null : CancelProgress.of(c, tracker.get(c.id()).orElse(null));
    }

    /**
     * WHAT:   The flight being cancelled on this page.
     * PRE:    none
     * RETURN: its flight number, or ""
     *
     * @return the flight
     */
    public String getCancellingFlight()
    {
        BookingDto c = find(cancellingId);
        return c == null ? "" : c.flightNumber();
    }

    /**
     * WHAT:   The line under the title, e.g. "2 upcoming · 5 in total".
     * PRE:    none
     * RETURN: the summary
     *
     * @return the summary
     */
    public String getSummary()
    {
        long upcoming = trips.stream().filter(b -> "CONFIRMED".equals(b.status())).count();
        return upcoming + " upcoming · " + trips.size() + " in total";
    }

    /**
     * WHAT:   Whether the customer has no trips at all.
     * WHY:    Not named "empty": that is a reserved word in Jakarta EL
     *         (the empty operator), so #{tripsView.empty} would not parse.
     * RETURN: true when loaded and empty
     *
     * @return whether there are no trips
     */
    public boolean isNoTrips()
    {
        return loadError == null && trips.isEmpty();
    }

    /**
     * WHAT:   Why trips could not be loaded.
     * RETURN: the message, or null
     *
     * @return the error
     */
    public String getLoadError()
    {
        return loadError;
    }

    /**
     * WHAT:   The status line.
     * RETURN: the message, or null when there is nothing to say
     *
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * WHAT:   The status line's CSS classes.
     * RETURN: classes for its tone
     *
     * @return the classes
     */
    public String getMessageCss()
    {
        return switch (tone)
        {
            case GOOD -> "msg msg-good";
            case BAD -> "msg msg-bad";
            case WARN -> "msg msg-warn";
            case MUTED -> "msg";
        };
    }

    /**
     * WHAT:   Whether "behind the scenes" notes show on the cancellation steps.
     * RETURN: true if shown
     *
     * @return whether shown
     */
    public boolean isDetails()
    {
        return details;
    }

    /**
     * WHAT:   Shows or hides the notes (bound to the checkbox).
     * POST:   details updated
     *
     * @param details whether to show them
     */
    public void setDetails(boolean details)
    {
        this.details = details;
    }

    /**
     * WHAT:   The status pill for a booking.
     * PRE:    b is not null
     * RETURN: { text, css modifier }
     *
     * @param b the booking
     * @return the pill
     */
    private static String[] pill(BookingDto b)
    {
        return switch (b.status())
        {
            case "CONFIRMED" -> new String[] { "Confirmed", "pill-good" };
            case "PAYMENT_FAILED" -> new String[] { "Declined", "pill-bad" };
            case "PENDING" -> new String[] { "Paying…", "pill-info" };
            case "CANCELLING" -> new String[] { "Cancelling…", "pill-warn" };
            case "CANCELLED" -> new String[] { "Refunded", "pill-muted" };
            default -> new String[] { b.status(), "pill-muted" };
        };
    }

    /**
     * WHAT:   The explanatory line for a booking, if it needs one.
     * PRE:    b is not null
     * RETURN: the note, or null
     *
     * @param b the booking
     * @return the note
     */
    private static String note(BookingDto b)
    {
        if ("PAYMENT_FAILED".equals(b.status()))
        {
            return "Card declined: " + b.failureReason() + ". No charge was made.";
        }
        if ("CANCELLED".equals(b.status()))
        {
            return "Cancelled. " + Money.format(b.price()) + " went back to your card.";
        }
        if (b.failureReason() != null && b.failureReason().startsWith(CancelProgress.REFUND_FAILED))
        {
            return "The refund didn't go through, so this ticket is still valid.";
        }
        return null;
    }

    /**
     * WHAT:   The cabin's display name.
     * PRE:    b is not null
     * RETURN: e.g. "Business"; the raw value if unknown
     *
     * @param b the booking
     * @return the cabin name
     */
    private static String cabin(BookingDto b)
    {
        try
        {
            return Cabin.valueOf(b.fareClass()).getLabel();
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return String.valueOf(b.fareClass());
        }
    }
}
