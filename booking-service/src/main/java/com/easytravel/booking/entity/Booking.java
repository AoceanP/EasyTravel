package com.easytravel.booking.entity;

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
 * JPA entity representing a booking as stored in the database.
 *
 * <p>This is an ENTITY, not a DTO — the distinction is deliberate. The entity is the
 * INTERNAL, persistence-mapped shape (it knows about tables, columns, the DB-generated
 * id). DTOs are the EXTERNAL contract. Keeping them separate means the table can evolve
 * without breaking the API, and the API never leaks storage details.
 *
 * <p>Unlike our DTOs (records), an entity is a mutable class with a no-arg constructor,
 * because JPA/Hibernate needs to instantiate it reflectively and set fields as it loads
 * rows. Records can't serve as JPA entities for that reason.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    /**
     * Primary key. IDENTITY strategy delegates id generation to the database's
     * auto-increment column — simple and standard for H2/PostgreSQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    /** Which flight was booked (references FlightDTO#flightNumber from inventory). */
    @Column(nullable = false)
    private String flightNumber;

    /**
     * SNAPSHOT of the price the customer agreed to, frozen at booking time. Deliberately
     * denormalized: a booking is a historical financial record, so it must preserve what
     * was agreed, independent of the live price which changes over time.
     */
    @Column(nullable = false)
    private BigDecimal agreedPrice;

    /**
     * The cabin booked. @Enumerated(STRING) stores the enum NAME ("BUSINESS") rather
     * than its ordinal position (1). Always use STRING for persisted enums: ordinals
     * break silently if you ever reorder or insert enum values, corrupting old rows.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FareClass fareClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. Kept protected: app code should use the
     other constructor, but Hibernate can still access this one. */
    protected Booking() {
    }

    /** Convenience constructor for creating a NEW booking in application code. */
    public Booking(String userId, String flightNumber, BigDecimal agreedPrice,
                   FareClass fareClass, BookingStatus status, Instant createdAt) {
        this.userId = userId;
        this.flightNumber = flightNumber;
        this.agreedPrice = agreedPrice;
        this.fareClass = fareClass;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters (and the setters JPA/logic need). Entities need accessors for Hibernate
    // and for serialization; kept minimal.
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getFlightNumber() { return flightNumber; }
    public BigDecimal getAgreedPrice() { return agreedPrice; }
    public FareClass getFareClass() { return fareClass; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(BookingStatus status) { this.status = status; }
}