package com.easytravel.web.gateway;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.function.Supplier;

/**
 * WHAT:   The pages' single way into the back end.
 * WHY:    Wraps GatewayClient so every failure, including "the gateway is not
 *         running at all", arrives as a GatewayException with a message a
 *         person can read.
 *
 * @author AoceanP
 */
@ApplicationScoped
public class TravelService
{
    /** WHAT: how often, and how long, to re-read a booking that payment has just decided. */
    private static final int DECISION_TRIES = 8;
    private static final long DECISION_PAUSE_MS = 120;

    /** WHAT: the REST client for the API gateway. */
    private final GatewayClient gateway;

    /**
     * WHAT:   Creates the service.
     * PRE:    CDI supplies the client
     *
     * @param gateway the REST client
     */
    @Inject
    public TravelService(@RestClient GatewayClient gateway)
    {
        this.gateway = gateway;
    }

    /**
     * WHAT:   No-argument constructor for the CDI proxy.
     * WHY:    Normal-scoped beans need one; it is never used to build a working service.
     */
    protected TravelService()
    {
        this.gateway = null;
    }

    /**
     * WHAT:   The flights on a route, best first.
     * PRE:    both codes are 3 letters
     * RETURN: the flights
     * THROWS: GatewayException if they cannot be loaded
     *
     * @param origin      e.g. "LHR"
     * @param destination e.g. "JFK"
     * @return the flights
     */
    public List<FlightDto> search(String origin, String destination)
    {
        return call(() -> gateway.search(origin, destination));
    }

    /**
     * WHAT:   Creates a booking.
     * PRE:    request is valid
     * RETURN: the PENDING booking
     * THROWS: GatewayException if it is refused or the gateway is down
     *
     * @param request the booking
     * @return the booking
     */
    public BookingDto book(BookingRequest request)
    {
        return call(() -> gateway.book(request));
    }

    /**
     * WHAT:   The current state of a booking.
     * PRE:    id is a booking id
     * RETURN: the booking
     * THROWS: GatewayException if it cannot be read
     *
     * @param id the booking id
     * @return the booking
     */
    public BookingDto booking(String id)
    {
        return call(() -> gateway.booking(id));
    }

    /**
     * WHAT:   A customer's bookings, newest first.
     * PRE:    userId is not blank
     * RETURN: the bookings
     * THROWS: GatewayException if they cannot be read
     *
     * @param userId the customer
     * @return the bookings
     */
    public List<BookingDto> bookingsFor(String userId)
    {
        return call(() -> gateway.bookings(userId));
    }

    /**
     * WHAT:   Starts cancelling a paid booking.
     * PRE:    id is a booking id
     * RETURN: the booking, now CANCELLING
     * THROWS: GatewayException if it cannot be cancelled (409) or found (404)
     *
     * @param id the booking id
     * @return the booking
     */
    public BookingDto cancel(String id)
    {
        return call(() -> gateway.cancel(id));
    }

    /**
     * WHAT:   Reads a booking whose refund result has arrived, waiting briefly
     *         until booking-service has applied it.
     * WHY:    Same race as awaitDecision: the website and booking-service hear
     *         the refund result at the same moment.
     * PRE:    payment has published the refund result for this booking
     * RETURN: the booking, out of CANCELLING if booking-service caught up in time
     * THROWS: GatewayException if it cannot be read
     *
     * @param id the booking id
     * @return the booking
     */
    public BookingDto awaitCancellation(String id)
    {
        BookingDto booking = booking(id);
        for (int i = 1; i < DECISION_TRIES && "CANCELLING".equals(booking.status()); i++)
        {
            pause();
            booking = booking(id);
        }
        return booking;
    }

    /**
     * WHAT:   Reads a booking that payment has already decided, waiting briefly
     *         until booking-service has saved the decision.
     * WHY:    The website hears the payment result at the same moment as
     *         booking-service does, so an immediate read can still say PENDING.
     *         A few short re-reads (under a second in total) close that gap.
     * PRE:    payment has published its result for this booking
     * RETURN: the booking, decided if booking-service caught up in time
     * THROWS: GatewayException if it cannot be read
     *
     * @param id the booking id
     * @return the booking
     */
    public BookingDto awaitDecision(String id)
    {
        BookingDto booking = booking(id);
        for (int i = 1; i < DECISION_TRIES && !booking.isDecided(); i++)
        {
            pause();
            booking = booking(id);
        }
        return booking;
    }

    /**
     * WHAT:   Runs one gateway call, translating connection failures.
     * PRE:    action is not null
     * RETURN: the call's result
     * THROWS: GatewayException for every kind of failure
     *
     * @param action the call
     * @param <T>    the result type
     * @return the result
     */
    private static <T> T call(Supplier<T> action)
    {
        try
        {
            return action.get();
        }
        catch (ProcessingException unreachable)
        {
            if (unreachable.getCause() instanceof GatewayException mapped)
            {
                throw mapped;
            }
            throw new GatewayException(503, "The booking system can't be reached right now. Try again shortly.");
        }
    }

    /**
     * WHAT:   Waits a moment between re-reads.
     * PRE:    none
     * POST:   the thread slept, unless interrupted (the flag is kept)
     */
    private static void pause()
    {
        try
        {
            Thread.sleep(DECISION_PAUSE_MS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
