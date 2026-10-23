package com.easytravel.payment.service;

import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import com.easytravel.payment.entity.Payment;
import com.easytravel.payment.gateway.MockPaymentGateway;
import com.easytravel.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WHAT:   Tests for PaymentService: charging, declines, and duplicate deliveries.
 * WHY:    Charging a customer twice is the worst bug a payment service can
 *         have, so idempotency is tested directly.
 *
 * @author AoceanP
 */
class PaymentServiceTest
{
    /** WHAT: the fake database. */
    private PaymentRepository repository;

    /** WHAT: the fake event bus. */
    private ApplicationEventPublisher events;

    /** WHAT: the service under test, with the real mock gateway (limit 2000.00). */
    private PaymentService service;

    /**
     * WHAT:   Builds the service; save() returns what it is given.
     * POST:   service is ready
     */
    @BeforeEach
    void setUp()
    {
        repository = mock(PaymentRepository.class);
        events = mock(ApplicationEventPublisher.class);
        service = new PaymentService(repository, new MockPaymentGateway(new BigDecimal("2000.00")), events,
                Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC));
        when(repository.findByBookingId(any())).thenReturn(Optional.empty());
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * WHAT:   A booking under the card limit.
     * WHY:    Saved as SUCCESS, and a success event is published for booking-service.
     * PRE:    none
     * RETURN: nothing; fails if the outcome or event is wrong
     */
    @Test
    void approvedPaymentIsAnnounced()
    {
        assertThat(service.processBooking(new BookingCreatedEvent("7", "alice", new BigDecimal("525.00")))).isTrue();

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().success()).isTrue();
        assertThat(event.getValue().bookingId()).isEqualTo("7");
        assertThat(event.getValue().transactionId()).isEqualTo(saved.getValue().getTransactionId());
    }

    /**
     * WHAT:   A booking over the card limit.
     * WHY:    The failure is saved AND published with its reason; the old
     *         code only published successes, leaving declined bookings PENDING forever.
     * PRE:    none
     * RETURN: nothing; fails if the failure is not announced
     */
    @Test
    void declinedPaymentIsAnnouncedToo()
    {
        service.processBooking(new BookingCreatedEvent("8", "bob", new BigDecimal("2100.00")));

        ArgumentCaptor<PaymentEvent> event = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().success()).isFalse();
        assertThat(event.getValue().reason()).isEqualTo("card limit of 2000.00 exceeded");
    }

    /**
     * WHAT:   The same booking delivered a second time.
     * WHY:    No second charge, no second event: the customer pays once.
     * PRE:    none
     * RETURN: nothing; fails if anything is saved or published
     */
    @Test
    void duplicateBookingIsNotChargedTwice()
    {
        Payment existing = new Payment("tx-1", "7", new BigDecimal("525.00"), PaymentStatus.SUCCESS, null, Instant.now());
        when(repository.findByBookingId("7")).thenReturn(Optional.of(existing));

        assertThat(service.processBooking(new BookingCreatedEvent("7", "alice", new BigDecimal("525.00")))).isFalse();
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any(Object.class));
    }

    /**
     * WHAT:   Looking up a payment that does not exist.
     * WHY:    A 404, not a 500.
     * PRE:    none
     * RETURN: nothing; fails unless ResourceNotFoundException is thrown
     */
    @Test
    void missingPaymentIsNotFound()
    {
        assertThatThrownBy(() -> service.getPaymentForBooking("99")).isInstanceOf(ResourceNotFoundException.class);
    }
}
