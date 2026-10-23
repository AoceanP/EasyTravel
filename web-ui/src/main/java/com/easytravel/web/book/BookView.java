package com.easytravel.web.book;

import com.easytravel.web.gateway.BookingDto;
import com.easytravel.web.gateway.BookingRequest;
import com.easytravel.web.gateway.FlightDto;
import com.easytravel.web.gateway.GatewayException;
import com.easytravel.web.gateway.TravelService;
import com.easytravel.web.live.BookingTracker;
import com.easytravel.web.live.BookingTracker.Track;
import com.easytravel.web.live.EventBridge;
import jakarta.annotation.PostConstruct;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * WHAT:   The Book page: pick a cabin, a flight and a seat, book, and watch the
 *         booking move through the saga.
 * WHY:    View-scoped: the choices live as long as the page is open and survive
 *         every ajax request, without a database. All prices and statuses shown
 *         after booking come from booking-service; the card prices before booking
 *         use the same rule the server uses.
 *
 * @author AoceanP
 */
@Named
@ViewScoped
public class BookView implements Serializable
{
    /** WHAT: the demo route. */
    static final String ORIGIN = "LHR";
    static final String DESTINATION = "JFK";

    /** WHAT: the message shown before anything happens. */
    private static final String READY = "Pick a seat, then book. Nothing is charged for real.";

    /**
     * WHAT:   The look of the status line under the ticket.
     */
    public enum Tone
    {
        MUTED("msg"), GOOD("msg msg-good"), BAD("msg msg-bad"), WARN("msg msg-warn");

        /** WHAT: CSS classes. */
        private final String css;

        /**
         * WHAT:   Creates a tone.
         * PRE:    none
         *
         * @param css CSS classes
         */
        Tone(String css)
        {
            this.css = css;
        }
    }

    @Inject
    TravelService travel;

    @Inject
    BookingTracker tracker;

    @Inject
    EventBridge events;

    @Inject
    Visitor visitor;

    /** WHAT: the mock card's limit, as configured in payment-service (demo only). */
    @Inject
    @ConfigProperty(name = "easytravel.card-limit", defaultValue = "2000.00")
    String cardLimit;

    /** WHAT: the flights on the route, in the server's ranked order. */
    private List<FlightDto> flights = List.of();

    /** WHAT: why flights could not be loaded, or null. */
    private String loadError;

    private Cabin cabin = Cabin.ECONOMY;
    private SortOrder sort = SortOrder.BEST;
    private String flight;
    private String seat;

    /** WHAT: the booking made on this page, as last read from the gateway, or null. */
    private BookingDto booking;

    private String message = READY;
    private Tone tone = Tone.MUTED;
    private boolean details;

    /**
     * WHAT:   Loads the flights when the page opens.
     * PRE:    called once by CDI
     * POST:   flights are loaded and the best one is chosen, or loadError is set
     */
    @PostConstruct
    void init()
    {
        load();
    }

    /**
     * WHAT:   (Re)loads the flights; also the "Try again" button.
     * PRE:    none
     * POST:   flights and loadError are updated
     */
    public void load()
    {
        try
        {
            flights = travel.search(ORIGIN, DESTINATION);
            loadError = flights.isEmpty() ? "No flights on this route right now." : null;
            if (flight == null && !flights.isEmpty())
            {
                flight = flights.getFirst().flightNumber();
            }
        }
        catch (GatewayException e)
        {
            flights = List.of();
            loadError = e.getMessage();
        }
    }

    // ------------------------------------------------------------- actions

    /**
     * WHAT:   Chooses a cabin.
     * PRE:    value is a Cabin name
     * POST:   cabin changed; seat and any finished booking cleared
     *
     * @param value the cabin
     */
    public void pickCabin(String value)
    {
        if (isBusy()) return;
        cabin = Cabin.valueOf(value);
        startOver();
    }

    /**
     * WHAT:   Chooses how flights are sorted.
     * PRE:    value is a SortOrder name
     * POST:   sort changed
     *
     * @param value the order
     */
    public void pickSort(String value)
    {
        sort = SortOrder.valueOf(value);
    }

    /**
     * WHAT:   Chooses a flight.
     * PRE:    code is one of the loaded flights
     * POST:   flight changed; seat and any finished booking cleared
     *
     * @param code the flight number
     */
    public void pickFlight(String code)
    {
        if (isBusy()) return;
        flight = code;
        startOver();
    }

    /**
     * WHAT:   Chooses a seat.
     * WHY:    A taken seat explains itself instead of doing nothing.
     * PRE:    id is a seat on the current map
     * POST:   seat chosen, or a message says why not
     *
     * @param id the seat
     */
    public void pickSeat(String id)
    {
        if (isBusy()) return;
        SeatMap.Seat picked = SeatMap.find(getSeatRows(), id);
        if (picked == null) return;
        if (picked.isTaken())
        {
            say("Seat " + id + " is taken. Pick a free one.", Tone.BAD);
            return;
        }
        if (booking != null)
        {
            booking = null;
        }
        seat = id;
        say("Seat " + id + " selected. Ready when you are.", Tone.MUTED);
    }

    /**
     * WHAT:   The ticket's main button: books, or starts over after a result.
     * PRE:    none
     * POST:   a PENDING booking exists and is being watched, or a message says why not
     */
    public void book()
    {
        if (isBusy()) return;
        if (booking != null)
        {
            startOver();
            return;
        }
        if (seat == null)
        {
            say("Pick a seat first.", Tone.BAD);
            return;
        }
        try
        {
            booking = travel.book(new BookingRequest(visitor.getUserId(), flight, ORIGIN, DESTINATION, cabin.name()));
            tracker.watch(booking.id(), visitor.getKey());
            say("Booking saved. Waiting for payment…", Tone.MUTED);
            // the payment result may already have arrived while we waited for the response
            tracker.get(booking.id()).filter(t -> t.paid() != null).ifPresent(t -> refresh());
        }
        catch (GatewayException e)
        {
            say(e.getMessage(), Tone.BAD);
        }
    }

    /**
     * WHAT:   Handles the live push: the browser was told its booking changed.
     * WHY:    Faces calls an f:ajax listener with the ajax event, so the push
     *         gets its own method with that parameter; it simply re-reads.
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
     * WHAT:   Re-reads the booking: called by onLiveUpdate, and by "Check status".
     * PRE:    none
     * POST:   booking and message reflect booking-service's current state
     */
    public void refresh()
    {
        if (booking == null) return;
        try
        {
            boolean paid = tracker.get(booking.id()).map(t -> t.paid() != null).orElse(false);
            booking = paid ? travel.awaitDecision(booking.id()) : travel.booking(booking.id());
            if ("CONFIRMED".equals(booking.status()))
            {
                say("You're booked. Seat " + seat + " on " + booking.flightNumber() + " is yours.", Tone.GOOD);
            }
            else if ("PAYMENT_FAILED".equals(booking.status()))
            {
                say("Card declined (" + booking.failureReason() + "), so no charge was made.", Tone.BAD);
            }
        }
        catch (GatewayException e)
        {
            say(e.getMessage(), Tone.BAD);
        }
    }

    /**
     * WHAT:   Clears the seat and any booking, keeping cabin, flight and sort.
     * PRE:    none
     * POST:   ready for a new booking
     */
    private void startOver()
    {
        booking = null;
        seat = null;
        say(READY, Tone.MUTED);
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
     * WHAT:   The cabin chips.
     * PRE:    none
     * RETURN: one chip per cabin
     *
     * @return the chips
     */
    public List<Chip> getCabins()
    {
        List<Chip> chips = new ArrayList<>();
        for (Cabin c : Cabin.values())
        {
            chips.add(new Chip(c.name(), c.getLabel(), c == cabin ? "chip chip-on" : "chip", c == cabin));
        }
        return chips;
    }

    /**
     * WHAT:   The sort chips.
     * PRE:    none
     * RETURN: one chip per order
     *
     * @return the chips
     */
    public List<Chip> getSorts()
    {
        List<Chip> chips = new ArrayList<>();
        for (SortOrder s : SortOrder.values())
        {
            chips.add(new Chip(s.name(), s.getLabel(), s == sort ? "chip chip-on" : "chip", s == sort));
        }
        return chips;
    }

    /**
     * WHAT:   The flight cards, in the chosen order and priced for the chosen cabin.
     * PRE:    none
     * RETURN: the cards; empty if flights could not be loaded
     *
     * @return the cards
     */
    public List<FlightCard> getFlights()
    {
        List<FlightDto> ordered = sort.apply(flights);
        int longest = ordered.stream().mapToInt(FlightDto::durationMinutes).max().orElse(1);
        BigDecimal limit = new BigDecimal(cardLimit);
        List<FlightCard> cards = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++)
        {
            FlightDto f = ordered.get(i);
            BigDecimal price = cabin.price(f.price());
            boolean over = price.compareTo(limit) > 0;
            boolean selected = f.flightNumber().equals(flight);
            cards.add(new FlightCard(f.flightNumber(), Airlines.name(f.flightNumber()), Airlines.code(f.flightNumber()),
                    "mono " + Airlines.css(f.flightNumber()), Money.format(price), Money.duration(f.durationMinutes()),
                    Math.round(100.0 * f.durationMinutes() / longest) + "%", i == 0 ? sort.getTag() : null,
                    over ? "over card limit" : "per person", over ? "note note-warn" : "note",
                    selected ? "flight flight-on" : "flight", selected));
        }
        return cards;
    }

    /**
     * WHAT:   The seat grid for the chosen flight and cabin.
     * PRE:    none
     * RETURN: the rows; the chosen seat shows as "yours" once confirmed
     *
     * @return the rows
     */
    public List<SeatMap.Row> getSeatRows()
    {
        if (flight == null) return List.of();
        boolean confirmed = booking != null && "CONFIRMED".equals(booking.status());
        String shown = booking != null && "PAYMENT_FAILED".equals(booking.status()) ? null : seat;
        return SeatMap.rows(flight, cabin, shown, confirmed ? SeatMap.State.YOURS : SeatMap.State.SELECTED);
    }

    /**
     * WHAT:   The ticket's progress.
     * PRE:    none
     * RETURN: the four steps and whether the booking is decided
     *
     * @return the progress
     */
    public BookingProgress getProgress()
    {
        Track track = booking == null ? null : tracker.get(booking.id()).orElse(null);
        return BookingProgress.of(booking, track);
    }

    /**
     * WHAT:   Whether a booking is waiting for payment.
     * WHY:    Choices are locked meanwhile, so the ticket cannot change under it.
     * PRE:    none
     * RETURN: true while PENDING
     *
     * @return whether busy
     */
    public boolean isBusy()
    {
        return booking != null && !booking.isDecided();
    }

    /**
     * WHAT:   The ticket's total: the server's price once booked, else the card price.
     * PRE:    none
     * RETURN: the formatted amount, or "" before flights load
     *
     * @return the total
     */
    public String getTotal()
    {
        if (booking != null) return Money.format(booking.price());
        return flights.stream().filter(f -> f.flightNumber().equals(flight)).findFirst()
                .map(f -> Money.format(cabin.price(f.price()))).orElse("");
    }

    /**
     * WHAT:   The ticket's main button text.
     * PRE:    none
     * RETURN: "Book flight", "Booking…" or "Book another"
     *
     * @return the label
     */
    public String getBookLabel()
    {
        return booking == null ? "Book flight" : isBusy() ? "Booking…" : "Book another";
    }

    /**
     * WHAT:   The status line, warning before booking when the price is over the card limit.
     * PRE:    none
     * RETURN: the text
     *
     * @return the message
     */
    public String getMessage()
    {
        return overLimitWarning() ? "Heads up: " + getTotal() + " is over the demo card limit of "
                + Money.format(new BigDecimal(cardLimit)) + ", so payment will be declined." : message;
    }

    /**
     * WHAT:   The status line's CSS classes.
     * PRE:    none
     * RETURN: the classes for its tone
     *
     * @return the classes
     */
    public String getMessageCss()
    {
        return overLimitWarning() ? Tone.WARN.css : tone.css;
    }

    /**
     * WHAT:   Whether to warn about the card limit instead of the normal message.
     * PRE:    none
     * RETURN: true before booking, with no error showing, when the price is over the limit
     *
     * @return whether to warn
     */
    private boolean overLimitWarning()
    {
        if (booking != null || tone == Tone.BAD) return false;
        return flights.stream().filter(f -> f.flightNumber().equals(flight)).findFirst()
                .map(f -> cabin.price(f.price()).compareTo(new BigDecimal(cardLimit)) > 0).orElse(false);
    }

    /**
     * WHAT:   The stamp on a decided ticket.
     * PRE:    none
     * RETURN: "CONFIRMED", "DECLINED", or null while undecided
     *
     * @return the stamp text
     */
    public String getStamp()
    {
        if (booking == null || !booking.isDecided()) return null;
        return "CONFIRMED".equals(booking.status()) ? "CONFIRMED" : "DECLINED";
    }

    /**
     * WHAT:   Whether live updates are working.
     * WHY:    If RabbitMQ is unreachable the page says so and offers "Check status".
     * PRE:    none
     * RETURN: true while the website is connected to RabbitMQ
     *
     * @return whether live
     */
    public boolean isLive()
    {
        return events.isConnected();
    }

    /**
     * WHAT:   Why flights could not be loaded.
     * RETURN: the message, or null when they loaded
     *
     * @return the error
     */
    public String getLoadError()
    {
        return loadError;
    }

    /**
     * WHAT:   The chosen cabin.
     * RETURN: the cabin
     *
     * @return the cabin
     */
    public Cabin getCabin()
    {
        return cabin;
    }

    /**
     * WHAT:   The chosen flight number.
     * RETURN: the flight, or null before flights load
     *
     * @return the flight
     */
    public String getFlight()
    {
        return flight;
    }

    /**
     * WHAT:   The chosen seat.
     * RETURN: e.g. "21C", or null
     *
     * @return the seat
     */
    public String getSeat()
    {
        return seat;
    }

    /**
     * WHAT:   The booking made on this page.
     * RETURN: the booking as last read, or null
     *
     * @return the booking
     */
    public BookingDto getBooking()
    {
        return booking;
    }

    /**
     * WHAT:   The chosen flight's airline.
     * RETURN: the airline name, or "" before flights load
     *
     * @return the airline
     */
    public String getAirline()
    {
        return flight == null ? "" : Airlines.name(flight);
    }

    /**
     * WHAT:   Whether "behind the scenes" details are shown on the ticket.
     * RETURN: true if shown
     *
     * @return whether shown
     */
    public boolean isDetails()
    {
        return details;
    }

    /**
     * WHAT:   Shows or hides the details (bound to the checkbox).
     * POST:   details updated
     *
     * @param details whether to show them
     */
    public void setDetails(boolean details)
    {
        this.details = details;
    }
}
