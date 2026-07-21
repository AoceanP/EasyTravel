package com.easytravel.booking.controller;

import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.FareClass;
import com.easytravel.booking.service.BookingService;
import com.easytravel.common.dto.BookingRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP edge for bookings.
 *
 * <p>@Valid on the @RequestBody is what TRIGGERS the Bean Validation rules we declared
 * on BookingRequestDTO back in Phase 1 (@NotBlank, @NotNull). If validation fails,
 * Spring throws MethodArgumentNotValidException — which our shared GlobalExceptionHandler
 * catches and turns into a clean 400. The rules live on the DTO; the trigger lives here.
 */
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Create a booking. fareClass comes as a query param, defaulting to ECONOMY.
     * Returns 201 Created with the persisted booking (now carrying its DB-generated id).
     * Example: POST /api/bookings?fareClass=BUSINESS  with a JSON body.
     */
    @PostMapping("/api/bookings")
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequestDTO request,
            @RequestParam(defaultValue = "ECONOMY") FareClass fareClass) {

        Booking created = bookingService.createBooking(request, fareClass);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/bookings/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    @GetMapping("/api/bookings")
    public List<Booking> getUserBookings(@RequestParam String userId) {
        return bookingService.getBookingsForUser(userId);
    }
}