package com.easytravel.booking.service;

import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.BookingStatus;
import com.easytravel.booking.entity.FareClass;
import com.easytravel.booking.repository.BookingRepository;
import com.easytravel.common.dto.BookingRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Booking business logic. This is where the incoming DTO becomes a persisted entity.
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Creates a new booking in PENDING status.
     *
     * <p>@Transactional wraps this in a database transaction: everything commits
     * together or rolls back together. For a single save it's arguably optional, but
     * it's the correct habit — as soon as a method does multiple writes, the
     * transaction boundary is what keeps your data consistent.
     *
     * <p>Note the DTO→entity translation happening here: we read the agreed price OFF
     * the request and SNAPSHOT it onto the entity. New bookings always start PENDING —
     * they only become CONFIRMED after payment (Phase 3.2).
     */
    @Transactional
    public Booking createBooking(BookingRequestDTO request, FareClass fareClass) {
        Booking booking = new Booking(
                request.userId(),
                request.flightNumber(),
                request.amount(),          // the agreed price, snapshotted now
                fareClass,
                BookingStatus.PENDING,     // every new booking starts here
                Instant.now()
        );
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
    }
}