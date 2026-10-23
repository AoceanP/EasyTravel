package com.easytravel.booking.service;

import com.easytravel.booking.client.InventoryClient;
import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.BookingStatus;
import com.easytravel.booking.repository.BookingRepository;
import com.easytravel.common.dto.BookingRequestDTO;
import com.easytravel.common.dto.FareClass;
import com.easytravel.common.dto.FlightDTO;
import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WHAT:   Tests for BookingService: pricing, the PENDING start, the event
 *         that starts payment, and applying payment results.
 * WHY:    This class is where both ends of the saga live, so every
 *         transition (and every duplicate) is checked.
 *
 * @author AoceanP
 */
class BookingServiceTest
{
    /** WHAT: a fixed time, so timestamps can be asserted exactly. */
    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");

    /** WHAT: the id the fake database gives every saved booking. */
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    /** WHAT: an id no booking has. */
    private static final UUID MISSING = UUID.fromString("00000000-0000-0000-0000-000000000099");

    /** WHAT: the fake database. */
    private BookingRepository repository;

    /** WHAT: the fake inventory service. */
    private InventoryClient inventory;

    /** WHAT: the fake event bus. */
    private ApplicationEventPublisher events;

    /** WHAT: the service under test. */
    private BookingService service;

    /**
     * WHAT:   Builds the service with fakes; save() assigns ID like a database would.
     * POST:   service is ready
     */
    @BeforeEach
    void setUp()
    {
        repository = mock(BookingRepository.class);
        inventory = mock(InventoryClient.class);
        events = mock(ApplicationEventPublisher.class);
        service = new BookingService(repository, inventory, events, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.save(any(Booking.class))).thenAnswer(call -> {
            Booking saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", ID);
            return saved;
        });
        when(inventory.quote("BA249", "LHR", "JFK"))
                .thenReturn(new FlightDTO("BA249", "LHR", "JFK", new BigDecimal("210.00"), 870));
    }

    /**
     * WHAT:   A BUSINESS booking on a 210.00 flight.
     * WHY:    The price must come from inventory times the cabin multiplier
     *         (210.00 x 2.5 = 525.00), the booking must start PENDING, and a
     *         BookingCreatedEvent with that price must be handed out for payment.
     * PRE:    none
     * RETURN: nothing; fails if the price, status or event is wrong
     */
    @Test
    void createBookingPricesFromInventoryAndAnnouncesIt()
    {
        Booking booking = service.createBooking(request(FareClass.BUSINESS));

        assertThat(booking.getAgreedPrice()).isEqualByComparingTo("525.00");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getCreatedAt()).isEqualTo(NOW);

        ArgumentCaptor<BookingCreatedEvent> event = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue()).isEqualTo(new BookingCreatedEvent(ID.toString(), "alice", new BigDecimal("525.00")));
    }

    /**
     * WHAT:   Prices for every cabin.
     * WHY:    ECONOMY keeps the base fare; FIRST is 5 times it, rounded to cents.
     * PRE:    none
     * RETURN: nothing; fails if a price is wrong
     */
    @Test
    void everyCabinIsPricedCorrectly()
    {
        assertThat(service.createBooking(request(FareClass.ECONOMY)).getAgreedPrice()).isEqualByComparingTo("210.00");
        assertThat(service.createBooking(request(FareClass.FIRST)).getAgreedPrice()).isEqualByComparingTo("1050.00");
    }

    /**
     * WHAT:   A flight inventory does not know.
     * WHY:    Nothing may be saved or announced: no booking, no payment.
     * PRE:    none
     * RETURN: nothing; fails if anything is saved or published
     */
    @Test
    void unknownFlightCreatesNothing()
    {
        when(inventory.quote("ZZ999", "LHR", "JFK")).thenThrow(new ResourceNotFoundException("flight ZZ999 not found"));
        BookingRequestDTO request = new BookingRequestDTO("alice", "ZZ999", "LHR", "JFK", FareClass.ECONOMY);

        assertThatThrownBy(() -> service.createBooking(request)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    /**
     * WHAT:   A successful payment for a PENDING booking.
     * WHY:    The booking becomes CONFIRMED and remembers the transaction.
     * PRE:    none
     * RETURN: nothing; fails if the status or transaction is wrong
     */
    @Test
    void successfulPaymentConfirms()
    {
        Booking booking = pendingBooking();

        assertThat(service.applyPaymentResult(new PaymentEvent(ID.toString(), "tx-1", true, booking.getAgreedPrice(), null))).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTransactionId()).isEqualTo("tx-1");
    }

    /**
     * WHAT:   A declined payment.
     * WHY:    The booking becomes PAYMENT_FAILED with the reason, instead of
     *         staying PENDING forever (the old behaviour).
     * PRE:    none
     * RETURN: nothing; fails if the status or reason is wrong
     */
    @Test
    void declinedPaymentFails()
    {
        Booking booking = pendingBooking();

        service.applyPaymentResult(new PaymentEvent(ID.toString(), "tx-2", false, booking.getAgreedPrice(), "card limit exceeded"));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(booking.getFailureReason()).isEqualTo("card limit exceeded");
    }

    /**
     * WHAT:   The same payment result delivered twice, then a contradicting one.
     * WHY:    Brokers deliver "at least once": duplicates must be ignored,
     *         and a late "failed" must never un-confirm a paid booking.
     * PRE:    none
     * RETURN: nothing; fails if a decided booking changes
     */
    @Test
    void duplicateAndLateResultsAreIgnored()
    {
        Booking booking = pendingBooking();
        PaymentEvent success = new PaymentEvent(ID.toString(), "tx-1", true, booking.getAgreedPrice(), null);

        assertThat(service.applyPaymentResult(success)).isTrue();
        assertThat(service.applyPaymentResult(success)).isFalse();
        assertThat(service.applyPaymentResult(new PaymentEvent(ID.toString(), "tx-9", false, booking.getAgreedPrice(), "late"))).isFalse();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTransactionId()).isEqualTo("tx-1");
    }

    /**
     * WHAT:   A payment result for a booking that does not exist, and one whose id is not a UUID.
     * WHY:    Both must fail clearly, so the listener can dead-letter them.
     * PRE:    none
     * RETURN: nothing; fails unless both throw ResourceNotFoundException
     */
    @Test
    void unknownBookingIsNotFound()
    {
        when(repository.findById(MISSING)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.applyPaymentResult(new PaymentEvent(MISSING.toString(), "tx", true, BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.applyPaymentResult(new PaymentEvent("abc", "tx", true, BigDecimal.TEN, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /**
     * WHAT:   A request for alice, LHR to JFK on BA249.
     * PRE:    fareClass is not null
     * RETURN: the request
     *
     * @param fareClass the cabin
     * @return the request
     */
    private static BookingRequestDTO request(FareClass fareClass)
    {
        return new BookingRequestDTO("alice", "BA249", "LHR", "JFK", fareClass);
    }

    /**
     * WHAT:   A PENDING booking with id ID, returned by the fake repository.
     * PRE:    setUp ran
     * RETURN: the booking
     *
     * @return the booking
     */
    private Booking pendingBooking()
    {
        Booking booking = new Booking("alice", "BA249", "LHR", "JFK", new BigDecimal("210.00"), FareClass.ECONOMY, NOW);
        ReflectionTestUtils.setField(booking, "id", ID);
        when(repository.findById(ID)).thenReturn(Optional.of(booking));
        return booking;
    }
}
