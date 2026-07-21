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
 * JPA entity for a payment attempt against a booking.
 *
 * <p>Reuses the PaymentStatus enum from the shared PaymentResponseDTO in common — one
 * definition of "what states a payment can be in", shared across the DTO contract and
 * the stored entity. That's the shared-kernel paying off again.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A stable, externally-meaningful transaction reference (UUID). Distinct from the
     DB id: the id is internal, the transactionId is what you'd show a customer or
     reconcile against a gateway. */
    @Column(nullable = false, unique = true)
    private String transactionId;

    /** Which booking this payment settles. Links the two services' data by id — NOT a
     foreign key (they're separate databases in separate services); just a reference. */
    @Column(nullable = false)
    private String bookingId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Instant processedAt;

    protected Payment() {
    }

    public Payment(String transactionId, String bookingId, BigDecimal amount,
                   PaymentStatus status, Instant processedAt) {
        this.transactionId = transactionId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = status;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public Instant getProcessedAt() { return processedAt; }
}