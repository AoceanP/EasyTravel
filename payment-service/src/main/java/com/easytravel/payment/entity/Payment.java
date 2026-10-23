package com.easytravel.payment.entity;

import com.easytravel.common.dto.PaymentResponseDTO.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * WHAT:   One payment attempt for a booking, as stored in the database.
 * WHY:    bookingId is UNIQUE: the database itself refuses a second payment
 *         for the same booking, even if two duplicate messages are processed
 *         at the same moment. Code checks are good; a database constraint
 *         cannot be raced.
 *
 * @author AoceanP
 */
@Entity
@Table(name = "payments")
public class Payment
{
    /** WHAT: the internal primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WHAT:   The public transaction reference (a UUID).
     * WHY:    Separate from the database id: the id is internal, this is
     *         what a customer would quote or a processor would reconcile.
     */
    @Column(nullable = false, unique = true)
    private String transactionId;

    /**
     * WHAT:   The booking this payment is for.
     * WHY:    A plain reference, not a foreign key: the bookings table lives
     *         in another service's database.
     */
    @Column(nullable = false, unique = true)
    private String bookingId;

    /** WHAT: the amount charged. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** WHAT: SUCCESS or FAILED. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** WHAT: why it failed, or null. */
    private String reason;

    /** WHAT: when it was processed. */
    @Column(nullable = false)
    private Instant processedAt;

    /**
     * WHAT:   The no-argument constructor Hibernate needs.
     */
    protected Payment()
    {
    }

    /**
     * WHAT:   Records a processed payment.
     * PRE:    every argument except reason is not null
     *
     * @param transactionId the public reference
     * @param bookingId     the booking paid for
     * @param amount        the amount
     * @param status        the outcome
     * @param reason        why it failed, or null
     * @param processedAt   when it happened
     */
    public Payment(String transactionId, String bookingId, BigDecimal amount, PaymentStatus status,
                   String reason, Instant processedAt)
    {
        this.transactionId = transactionId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.processedAt = processedAt;
    }

    /** @return the database id */
    public Long getId() { return id; }

    /** @return the public reference */
    public String getTransactionId() { return transactionId; }

    /** @return the booking paid for */
    public String getBookingId() { return bookingId; }

    /** @return the amount */
    public BigDecimal getAmount() { return amount; }

    /** @return the outcome */
    public PaymentStatus getStatus() { return status; }

    /** @return why it failed, or null */
    public String getReason() { return reason; }

    /** @return when it was processed */
    public Instant getProcessedAt() { return processedAt; }
}
