package com.easytravel.booking.controller;

import com.easytravel.booking.dto.BookingResponseDTO;
import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.service.BookingService;
import com.easytravel.common.dto.BookingRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * WHAT:   The HTTP edge of the booking service.
 * WHY:    @Valid on the request body runs the Bean Validation rules declared
 *         on BookingRequestDTO; a failure becomes a 400 listing every bad
 *         field (via the shared GlobalExceptionHandler). The rules live on
 *         the DTO, the trigger lives here.
 *
 *         POST /api/bookings              create; the new booking starts PENDING
 *         GET  /api/bookings/{id}         one booking
 *         POST /api/bookings/{id}/cancel  cancel a paid booking; it becomes CANCELLING, then CANCELLED
 *         GET  /api/bookings?userId=alice a customer's bookings
 *
 * @author AoceanP
 */
@RestController
@Validated
public class BookingController
{
    /** WHAT: does the actual work. */
    private final BookingService bookingService;

    /**
     * WHAT:   Creates the controller.
     * PRE:    bookingService is not null
     *
     * @param bookingService the booking logic
     */
    public BookingController(BookingService bookingService)
    {
        this.bookingService = bookingService;
    }

    /**
     * WHAT:   Creates a booking.
     * WHY:    Answers 201 Created with a Location header pointing at the new
     *         booking. The booking is PENDING: the client polls that URL to
     *         see it become CONFIRMED or PAYMENT_FAILED once payment finishes.
     * PRE:    the body passes validation
     * RETURN: 201 with the new booking
     *
     * @param request the booking request
     * @return the response
     */
    @PostMapping("/api/bookings")
    public ResponseEntity<BookingResponseDTO> createBooking(@Valid @RequestBody BookingRequestDTO request)
    {
        Booking booking = bookingService.createBooking(request);
        // Relative on purpose: behind the gateway an absolute URL would name
        // this container's private address, which the client cannot reach.
        URI location = URI.create("/api/bookings/" + booking.getId());
        return ResponseEntity.created(location).body(BookingResponseDTO.from(booking));
    }

    /**
     * WHAT:   Returns one booking.
     * PRE:    id is a UUID (otherwise 400)
     * RETURN: the booking, or 404
     *
     * @param id the booking id
     * @return the booking
     */
    @GetMapping("/api/bookings/{id}")
    public BookingResponseDTO getBooking(@PathVariable UUID id)
    {
        return BookingResponseDTO.from(bookingService.getBooking(id));
    }

    /**
     * WHAT:   Cancels a paid booking and starts its refund.
     * WHY:    202 Accepted, not 200: the cancellation has started but finishes
     *         later, when payment-service reports the refund. The body is the
     *         booking, now CANCELLING; the client follows it like a new booking.
     * PRE:    id is a UUID (otherwise 400)
     * RETURN: 202 with the booking; 404 if unknown; 409 if it cannot be cancelled now
     *
     * @param id the booking id
     * @return the response
     */
    @PostMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<BookingResponseDTO> cancelBooking(@PathVariable UUID id)
    {
        return ResponseEntity.accepted().body(BookingResponseDTO.from(bookingService.cancelBooking(id)));
    }

    /**
     * WHAT:   Returns a customer's bookings, newest first.
     * PRE:    userId is not blank
     * RETURN: the bookings; an empty list if there are none
     *
     * @param userId the customer
     * @return the bookings
     */
    @GetMapping("/api/bookings")
    public List<BookingResponseDTO> getUserBookings(@RequestParam @NotBlank String userId)
    {
        return bookingService.getBookingsForUser(userId).stream().map(BookingResponseDTO::from).toList();
    }
}
