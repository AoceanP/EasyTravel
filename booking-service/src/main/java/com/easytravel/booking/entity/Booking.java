package com.easytravel.booking.entity;

import com.easytravel.common.dto.FareClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * WHAT:   A booking as stored in the database.
 * WHY:    An ENTITY, not a DTO: it knows about tables, columns and the
 *         generated id, and it never leaves the service (BookingResponseDTO
 *         is what clients see). It is a mutable class with a no-argument
 *         constructor because Hibernate creates it by reflection, which a
 *         Java record cannot support.
 *         State changes go through methods (confirm, markPaymentFailed,
 *         requestCancel, markCancelled, markRefundFailed) that each check the
 *         starting status, so the rules live in one place.
 *
 * @author AoceanP
 */
@Entity
@Table(name = "bookings")
public class Booking
{
    /**
     * WHAT:   The primary key, a random UUID created when the booking is saved.
     * WHY:    payment-service uses the booking id to refuse charging twice. A
     *         counter (1, 2, 3...) restarts when this database does, so a NEW
     *         booking 1 would look like an old one and never be charged. A
     *         UUID is never reused, here or in any other service.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * WHAT:   Optimistic-locking version, incremented on every update.
     * WHY:    If two threads change the same booking at once, the second
     *         save fails instead of silently overwriting the first.
     */
    @Version
    private long version;

    /** WHAT: the customer who booked. */
    @Column(nullable = false)
    private String userId;

    /** WHAT: the flight booked, e.g. "BA249". */
    @Column(nullable = false)
    private String flightNumber;

    /** WHAT: departure airport. */
    @Column(nullable = false, length = 3)
    private String origin;

    /** WHAT: arrival airport. */
    @Column(nullable = false, length = 3)
    private String destination;

    /**
     * WHAT:   The price agreed at booking time, frozen.
     * WHY:    A booking is a financial record: it must keep what was agreed,
     *         even if the flight's price changes later.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal agreedPrice;

    /**
     * WHAT:   The cabin booked.
     * WHY:    Stored as the NAME ("BUSINESS"), never the position (1):
     *         positions silently break old rows if the enum is reordered.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FareClass fareClass;

    /** WHAT: where the booking is in its life (see BookingStatus). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    /** WHAT: the payment's transaction id, once a payment result arrived. */
    private String transactionId;

    /** WHAT: why the payment failed, if it did. */
    private String failureReason;

    /** WHAT: when the booking was created. */
    @Column(nullable = false)
    private Instant createdAt;

    /** WHAT: when the status last changed. */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * WHAT:   The no-argument constructor Hibernate needs.
     * WHY:    Protected so application code uses the real constructor.
     */
    protected Booking()
    {
    }

    /**
     * WHAT:   Creates a new booking, always PENDING.
     * WHY:    A booking is only CONFIRMED after a payment event, never at creation.
     * PRE:    every argument is not null; agreedPrice is the server-computed price
     *
     * @param userId       the customer
     * @param flightNumber the flight
     * @param origin       departure airport
     * @param destination  arrival airport
     * @param agreedPrice  the price to charge
     * @param fareClass    the cabin
     * @param now          the creation time
     */
    public Booking(String userId, String flightNumber, String origin, String destination,
                   BigDecimal agreedPrice, FareClass fareClass, Instant now)
    {
        this.userId = userId;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.agreedPrice = agreedPrice;
        this.fareClass = fareClass;
        this.status = BookingStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Marks the booking paid.
     * PRE:    the booking is PENDING
     * POST:   status is CONFIRMED and the transaction id is recorded
     * THROWS: IllegalStateException if the booking is not PENDING
     *
     * @param transactionId the payment's transaction id
     * @param now           the time of the change
     * @throws IllegalStateException if the booking already has a final status
     */
    public void confirm(String transactionId, Instant now)
    {
        require(BookingStatus.PENDING);
        this.status = BookingStatus.CONFIRMED;
        this.transactionId = transactionId;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Marks the payment as declined.
     * PRE:    the booking is PENDING
     * POST:   status is PAYMENT_FAILED with the reason recorded
     * THROWS: IllegalStateException if the booking is not PENDING
     *
     * @param transactionId the payment's transaction id
     * @param reason        why the payment failed
     * @param now           the time of the change
     * @throws IllegalStateException if the booking already has a final status
     */
    public void markPaymentFailed(String transactionId, String reason, Instant now)
    {
        require(BookingStatus.PENDING);
        this.status = BookingStatus.PAYMENT_FAILED;
        this.transactionId = transactionId;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Starts cancelling a paid booking.
     * PRE:    the booking is CONFIRMED
     * POST:   status is CANCELLING; any earlier refund failure note is cleared
     * THROWS: IllegalStateException if the booking is not CONFIRMED
     *
     * @param now the time of the change
     * @throws IllegalStateException if the booking is not CONFIRMED
     */
    public void requestCancel(Instant now)
    {
        require(BookingStatus.CONFIRMED);
        this.status = BookingStatus.CANCELLING;
        this.failureReason = null;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Completes a cancellation once the refund went through.
     * PRE:    the booking is CANCELLING
     * POST:   status is CANCELLED
     * THROWS: IllegalStateException if the booking is not CANCELLING
     *
     * @param now the time of the change
     * @throws IllegalStateException if the booking is not CANCELLING
     */
    public void markCancelled(Instant now)
    {
        require(BookingStatus.CANCELLING);
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Undoes a cancellation whose refund failed.
     * WHY:    The customer still paid, so the ticket must stay valid: the
     *         booking goes back to CONFIRMED with the reason noted.
     * PRE:    the booking is CANCELLING
     * POST:   status is CONFIRMED with failureReason "refund failed: ..."
     * THROWS: IllegalStateException if the booking is not CANCELLING
     *
     * @param reason why the refund failed
     * @param now    the time of the change
     * @throws IllegalStateException if the booking is not CANCELLING
     */
    public void markRefundFailed(String reason, Instant now)
    {
        require(BookingStatus.CANCELLING);
        this.status = BookingStatus.CONFIRMED;
        this.failureReason = "refund failed: " + reason;
        this.updatedAt = now;
    }

    /**
     * WHAT:   Refuses a change that does not start from the expected status.
     * WHY:    Stops a late or duplicated message from moving a booking along
     *         a path it is not on, e.g. un-confirming a paid booking.
     * PRE:    none
     * THROWS: IllegalStateException if the status is not the expected one
     *
     * @param expected the status the change starts from
     * @throws IllegalStateException if the booking is in another status
     */
    private void require(BookingStatus expected)
    {
        if (status != expected)
        {
            throw new IllegalStateException("booking " + id + " is " + status + ", not " + expected);
        }
    }

    /** @return the id */
    public UUID getId() { return id; }

    /** @return the customer */
    public String getUserId() { return userId; }

    /** @return the flight */
    public String getFlightNumber() { return flightNumber; }

    /** @return the departure airport */
    public String getOrigin() { return origin; }

    /** @return the arrival airport */
    public String getDestination() { return destination; }

    /** @return the agreed price */
    public BigDecimal getAgreedPrice() { return agreedPrice; }

    /** @return the cabin */
    public FareClass getFareClass() { return fareClass; }

    /** @return the status */
    public BookingStatus getStatus() { return status; }

    /** @return the payment's transaction id, or null */
    public String getTransactionId() { return transactionId; }

    /** @return why the payment failed, or null */
    public String getFailureReason() { return failureReason; }

    /** @return when it was created */
    public Instant getCreatedAt() { return createdAt; }

    /** @return when the status last changed */
    public Instant getUpdatedAt() { return updatedAt; }
}
