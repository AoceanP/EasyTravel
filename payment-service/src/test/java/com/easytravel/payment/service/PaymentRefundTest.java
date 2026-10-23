package com.easytravel.payment.service;

import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import com.easytravel.common.event.RefundEvent;
import com.easytravel.common.event.RefundRequestedEvent;
import com.easytravel.payment.entity.Payment;
import com.easytravel.payment.gateway.PaymentGateway;
import com.easytravel.payment.gateway.PaymentGateway.ChargeResult;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WHAT:   Tests for refunds: the cancellation saga's payment side.
 * WHY:    A refund must happen at most once, and every request must get an
 *         answer, or a cancellation would hang in CANCELLING.
 *
 * @author AoceanP
 */
class PaymentRefundTest
{
    /** WHAT: a fixed time. */
    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    /** WHAT: the request every test sends. */
    private static final RefundRequestedEvent REQUEST = new RefundRequestedEvent("b-7", "tx-1", new BigDecimal("525.00"));

    private PaymentRepository repository;
    private PaymentGateway gateway;
    private ApplicationEventPublisher events;
    private PaymentService service;

    /**
     * WHAT:   Builds the service with fakes; the gateway approves refunds.
     * POST:   service is ready
     */
    @BeforeEach
    void setUp()
    {
        repository = mock(PaymentRepository.class);
        gateway = mock(PaymentGateway.class);
        events = mock(ApplicationEventPublisher.class);
        when(gateway.refund(any(), any(), any())).thenReturn(ChargeResult.approved());
        service = new PaymentService(repository, gateway, events, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /**
     * WHAT:   Refunding a successful payment.
     * WHY:    The money goes back, the payment is REFUNDED, success is announced.
     * RETURN: nothing; fails if any of the three is missing
     */
    @Test
    void refundsASuccessfulPayment()
    {
        Payment payment = stored(PaymentStatus.SUCCESS);

        assertThat(service.processRefund(REQUEST)).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(NOW);
        assertThat(announced().success()).isTrue();
    }

    /**
     * WHAT:   The same refund request delivered again after the refund.
     * WHY:    No second refund; success is announced again so a lost first
     *         announcement cannot leave the booking stuck.
     * RETURN: nothing; fails if the card is refunded twice
     */
    @Test
    void duplicateRequestNeverRefundsTwice()
    {
        stored(PaymentStatus.SUCCESS);
        service.processRefund(REQUEST);
        assertThat(service.processRefund(REQUEST)).isFalse();

        verify(gateway, times(1)).refund(any(), any(), any());
        verify(events, times(2)).publishEvent(any(RefundEvent.class));
    }

    /**
     * WHAT:   A refund request for a declined payment, and for no payment at all.
     * WHY:    Nothing to refund, but each still gets a failure answer.
     * RETURN: nothing; fails if the gateway is called or no answer is sent
     */
    @Test
    void nothingToRefundIsAnsweredAsAFailure()
    {
        stored(PaymentStatus.FAILED);
        service.processRefund(REQUEST);
        assertThat(announced().reason()).contains("declined");

        when(repository.findByBookingId("b-7")).thenReturn(Optional.empty());
        service.processRefund(REQUEST);
        verify(gateway, never()).refund(any(), any(), any());
    }

    /**
     * WHAT:   The processor refuses the refund.
     * WHY:    The payment stays SUCCESS, and the failure travels back with its reason.
     * RETURN: nothing; fails if the payment is marked refunded
     */
    @Test
    void refusedRefundIsReported()
    {
        Payment payment = stored(PaymentStatus.SUCCESS);
        when(gateway.refund(any(), any(), any())).thenReturn(ChargeResult.declined("card closed"));

        assertThat(service.processRefund(REQUEST)).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(announced().reason()).isEqualTo("card closed");
    }

    /**
     * WHAT:   A payment for booking "b-7" in the given status, returned by the fake repository.
     * PRE:    none
     * RETURN: the payment
     *
     * @param status its status
     * @return the payment
     */
    private Payment stored(PaymentStatus status)
    {
        Payment p = new Payment("tx-1", "b-7", new BigDecimal("525.00"), status, null, NOW);
        when(repository.findByBookingId("b-7")).thenReturn(Optional.of(p));
        return p;
    }

    /**
     * WHAT:   The last RefundEvent handed to the event bus.
     * PRE:    at least one was published
     * RETURN: the event
     *
     * @return the event
     */
    private RefundEvent announced()
    {
        ArgumentCaptor<RefundEvent> event = ArgumentCaptor.forClass(RefundEvent.class);
        verify(events, org.mockito.Mockito.atLeastOnce()).publishEvent(event.capture());
        return event.getValue();
    }
}
