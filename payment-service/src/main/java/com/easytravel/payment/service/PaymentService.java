package com.easytravel.payment.service;

import com.easytravel.common.dto.PaymentResponseDTO;
import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import com.easytravel.common.event.BookingCreatedEvent;
import com.easytravel.common.event.PaymentEvent;
import com.easytravel.common.event.RefundEvent;
import com.easytravel.common.event.RefundRequestedEvent;
import com.easytravel.common.exception.ResourceNotFoundException;
import com.easytravel.payment.entity.Payment;
import com.easytravel.payment.gateway.PaymentGateway;
import com.easytravel.payment.gateway.PaymentGateway.ChargeResult;
import com.easytravel.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * WHAT:   Charges new bookings and records the result.
 * WHY:    The middle step of the saga: a BookingCreatedEvent comes in, the
 *         card is charged, and a PaymentEvent (success OR failure) goes out.
 *
 * @author AoceanP
 */
@Service
public class PaymentService
{
    /** WHAT: logs each payment and every ignored duplicate. */
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /** WHAT: database access. */
    private final PaymentRepository paymentRepository;

    /** WHAT: the card processor. */
    private final PaymentGateway paymentGateway;

    /** WHAT: Spring's event bus; the PaymentEvent is sent to RabbitMQ after commit. */
    private final ApplicationEventPublisher events;

    /** WHAT: the clock; injected so tests can fix the time. */
    private final Clock clock;

    /**
     * WHAT:   Creates the service.
     * PRE:    all arguments are not null
     *
     * @param paymentRepository database access
     * @param paymentGateway    the card processor
     * @param events            Spring's event publisher
     * @param clock             the clock
     */
    public PaymentService(PaymentRepository paymentRepository, PaymentGateway paymentGateway,
                          ApplicationEventPublisher events, Clock clock)
    {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.events = events;
        this.clock = clock;
    }

    /**
     * WHAT:   Charges a new booking, saves the outcome, and announces it.
     * WHY:    IDEMPOTENT: brokers deliver "at least once", so the same
     *         booking can arrive twice. A booking that already has a payment
     *         is never charged again; the duplicate is simply ignored.
     * PRE:    event came from booking-service
     * POST:   exactly one Payment exists for the booking; a PaymentEvent is
     *         queued for after commit (unless this was a duplicate)
     * RETURN: true if a charge was made, false for a duplicate
     *
     * @param event the new booking
     * @return whether the booking was charged now
     */
    @Transactional
    public boolean processBooking(BookingCreatedEvent event)
    {
        // 1. already paid (or declined)? then this is a duplicate delivery: do nothing
        if (paymentRepository.findByBookingId(event.bookingId()).isPresent())
        {
            log.info("ignoring duplicate booking {}: already processed", event.bookingId());
            return false;
        }

        // 2. charge the card
        ChargeResult result = paymentGateway.charge(event.bookingId(), event.userId(), event.amount());

        // 3. record the outcome; the unique bookingId column stops a concurrent duplicate here
        Payment payment = paymentRepository.save(new Payment(UUID.randomUUID().toString(), event.bookingId(),
                event.amount(), result.success() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                result.reason(), Instant.now(clock)));

        // 4. announce the result (success AND failure), delivered after this transaction commits
        events.publishEvent(new PaymentEvent(payment.getBookingId(), payment.getTransactionId(),
                result.success(), payment.getAmount(), result.reason()));
        log.info("booking {}: payment {} {}", event.bookingId(), payment.getStatus(),
                result.success() ? "" : "(" + result.reason() + ")");
        return true;
    }

    /**
     * WHAT:   Refunds a cancelled booking's payment and announces the result.
     * WHY:    The middle step of the cancellation saga. IDEMPOTENT: a payment
     *         that is already REFUNDED is never refunded twice; the success is
     *         simply announced again, which is harmless (booking-service ignores
     *         it) and recovers the case where the first announcement was lost.
     *         Every other case is answered with a failure event, so the
     *         cancellation can never hang in CANCELLING.
     * PRE:    event came from booking-service
     * POST:   the payment is REFUNDED (if it was SUCCESS); a RefundEvent is
     *         queued for after commit in every case
     * RETURN: true if money was returned now, false otherwise
     *
     * @param event the refund request
     * @return whether a refund was made now
     */
    @Transactional
    public boolean processRefund(RefundRequestedEvent event)
    {
        Payment payment = paymentRepository.findByBookingId(event.bookingId()).orElse(null);
        if (payment == null)
        {
            announceRefund(event, false, "no payment found for this booking");
            return false;
        }
        switch (payment.getStatus())
        {
            case REFUNDED ->
            {
                log.info("booking {}: already refunded; announcing it again", event.bookingId());
                announceRefund(event, true, null);
                return false;
            }
            case FAILED ->
            {
                announceRefund(event, false, "the payment was declined, so there is nothing to refund");
                return false;
            }
            case SUCCESS ->
            {
                ChargeResult result = paymentGateway.refund(event.bookingId(), payment.getTransactionId(), payment.getAmount());
                if (result.success())
                {
                    payment.markRefunded(Instant.now(clock));
                }
                announceRefund(event, result.success(), result.reason());
                log.info("booking {}: refund of {} {}", event.bookingId(), payment.getAmount(),
                        result.success() ? "sent" : "failed (" + result.reason() + ")");
                return result.success();
            }
        }
        throw new IllegalStateException("unknown payment status " + payment.getStatus());
    }

    /**
     * WHAT:   Queues a RefundEvent for after this transaction commits.
     * PRE:    called inside a transaction
     * POST:   the event will be published if the transaction commits
     *
     * @param request the refund request being answered
     * @param success whether the refund went through
     * @param reason  why not, or null
     */
    private void announceRefund(RefundRequestedEvent request, boolean success, String reason)
    {
        events.publishEvent(new RefundEvent(request.bookingId(), request.transactionId(), success,
                request.amount(), reason));
    }

    /**
     * WHAT:   Returns the payment for a booking.
     * PRE:    bookingId is not null
     * RETURN: the payment
     * THROWS: ResourceNotFoundException if the booking has no payment (404)
     *
     * @param bookingId the booking
     * @return the payment
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentForBooking(String bookingId)
    {
        Payment p = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("no payment for booking " + bookingId));
        return new PaymentResponseDTO(p.getTransactionId(), p.getBookingId(), p.getStatus(), p.getAmount(),
                p.getReason(), p.getProcessedAt());
    }
}
