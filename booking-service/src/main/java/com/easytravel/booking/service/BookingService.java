package com.easytravel.booking.service;

import com.easytravel.booking.client.InventoryClient;
import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.BookingStatus;
import com.easytravel.booking.repository.BookingRepository;
import com.easytravel.common.dto.BookingRequestDTO;
import com.easytravel.common.dto.FlightDTO;
import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WHAT:   The booking business logic: create and price bookings, and apply
 *         payment results.
 * WHY:    This is where the saga starts and ends. Creating a booking saves
 *         it as PENDING and announces it; the payment result later moves it
 *         to CONFIRMED or PAYMENT_FAILED. The two services never call each
 *         other directly; they only exchange events.
 *
 * @author AoceanP
 */
@Service
public class BookingService
{
    /** WHAT: logs state changes and ignored duplicates. */
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /** WHAT: database access. */
    private final BookingRepository bookingRepository;

    /** WHAT: asks inventory-service for the real price. */
    private final InventoryClient inventoryClient;

    /** WHAT: Spring's in-process event bus, used to publish only after the transaction commits. */
    private final ApplicationEventPublisher events;

    /** WHAT: the clock; injected so tests can fix the time. */
    private final Clock clock;

    /**
     * WHAT:   Creates the service.
     * PRE:    all arguments are not null
     *
     * @param bookingRepository database access
     * @param inventoryClient   the price source
     * @param events            Spring's event publisher
     * @param clock             the clock to read the time from
     */
    public BookingService(BookingRepository bookingRepository, InventoryClient inventoryClient,
                          ApplicationEventPublisher events, Clock clock)
    {
        this.bookingRepository = bookingRepository;
        this.inventoryClient = inventoryClient;
        this.events = events;
        this.clock = clock;
    }

    /**
     * WHAT:   Prices and saves a new booking, then announces it for payment.
     * WHY:    The price comes from inventory-service, never from the client.
     *         The BookingCreatedEvent is handed to Spring, and
     *         BookingEventPublisher only sends it to RabbitMQ AFTER this
     *         transaction commits: if the save rolled back, no payment is
     *         ever taken for a booking that does not exist.
     * PRE:    request passed validation
     * POST:   a PENDING booking is saved; a BookingCreatedEvent is queued for after commit
     * RETURN: the saved booking, with its id
     * THROWS: ResourceNotFoundException if the flight does not exist;
     *         ServiceUnavailableException if inventory-service is down
     *
     * @param request the validated request
     * @return the saved booking
     */
    @Transactional
    public Booking createBooking(BookingRequestDTO request)
    {
        // 1. the real base fare, from the source of truth
        FlightDTO flight = inventoryClient.quote(request.flightNumber(), request.origin(), request.destination());

        // 2. apply the cabin multiplier; money is rounded to cents, half up, like a bank
        BigDecimal price = flight.price().multiply(request.fareClass().multiplier()).setScale(2, RoundingMode.HALF_UP);

        // 3. save as PENDING
        Booking booking = bookingRepository.save(new Booking(request.userId(), flight.flightNumber(),
                flight.origin(), flight.destination(), price, request.fareClass(), Instant.now(clock)));

        // 4. announce it; delivered to RabbitMQ only once the transaction has committed
        events.publishEvent(new BookingCreatedEvent(String.valueOf(booking.getId()), booking.getUserId(), price));
        log.info("booking {} created for {} at {} ({}), waiting for payment",
                booking.getId(), booking.getUserId(), price, booking.getFareClass());
        return booking;
    }

    /**
     * WHAT:   Returns one booking.
     * PRE:    id is not null
     * RETURN: the booking
     * THROWS: ResourceNotFoundException if it does not exist (answered as 404)
     *
     * @param id the booking id
     * @return the booking
     */
    @Transactional(readOnly = true)
    public Booking getBooking(UUID id)
    {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("booking " + id + " not found"));
    }

    /**
     * WHAT:   Returns a customer's bookings, newest first.
     * PRE:    userId is not null
     * RETURN: the bookings; empty if there are none
     *
     * @param userId the customer
     * @return the bookings
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(String userId)
    {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * WHAT:   Applies a payment result: CONFIRMED on success, PAYMENT_FAILED otherwise.
     * WHY:    Message brokers deliver "at least once", so the same event can
     *         arrive twice. This is IDEMPOTENT: a booking that already has a
     *         final status is left alone, so a duplicate (or a late
     *         contradicting message) can never change a decided booking.
     * PRE:    event is not null
     * POST:   a PENDING booking moves to its final status; anything else is unchanged
     * RETURN: true if the booking changed, false if the event was a duplicate
     * THROWS: ResourceNotFoundException if no such booking exists (the
     *         listener then sends the message to the dead-letter queue)
     *
     * @param event the payment result
     * @return whether the booking changed
     */
    @Transactional
    public boolean applyPaymentResult(PaymentEvent event)
    {
        UUID id = parseId(event.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("booking id '" + event.bookingId() + "' is not a valid id"));
        Booking booking = getBooking(id);

        if (booking.getStatus() != BookingStatus.PENDING)
        {
            log.info("ignoring payment result for booking {}: already {}", id, booking.getStatus());
            return false;
        }

        if (event.success())
        {
            booking.confirm(event.transactionId(), Instant.now(clock));
        }
        else
        {
            booking.markPaymentFailed(event.transactionId(), event.reason(), Instant.now(clock));
        }
        log.info("booking {} is now {}", id, booking.getStatus());
        // no save() needed: the entity is managed, so the change is flushed when the transaction commits
        return true;
    }

    /**
     * WHAT:   Reads a booking id from an event.
     * WHY:    Events are external input; a malformed id must not crash the listener.
     * PRE:    none
     * RETURN: the id, or empty if text is not a UUID
     *
     * @param text the id as text
     * @return the id
     */
    private static Optional<UUID> parseId(String text)
    {
        try
        {
            return Optional.of(UUID.fromString(text));
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return Optional.empty();
        }
    }
}
