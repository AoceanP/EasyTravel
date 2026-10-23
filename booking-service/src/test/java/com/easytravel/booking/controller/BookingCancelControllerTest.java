package com.easytravel.booking.controller;

import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.service.BookingService;
import com.easytravel.common.dto.FareClass;
import com.easytravel.common.exception.ConflictException;
import com.easytravel.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WHAT:   Tests for POST /api/bookings/{id}/cancel.
 *
 * @author AoceanP
 */
class BookingCancelControllerTest
{
    /** WHAT: the booking every test cancels. */
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    private BookingService service;
    private MockMvc mvc;

    /**
     * WHAT:   Wires the controller with a fake service and the shared error handler.
     * POST:   mvc is ready
     */
    @BeforeEach
    void setUp()
    {
        service = mock(BookingService.class);
        mvc = MockMvcBuilders.standaloneSetup(new BookingController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * WHAT:   Cancelling a paid booking.
     * WHY:    202 Accepted: the refund finishes later; the body is the CANCELLING booking.
     * RETURN: nothing; fails if the status or body is wrong
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void cancelIsAccepted() throws Exception
    {
        Booking b = new Booking("alice", "BA249", "LHR", "JFK", new BigDecimal("210.00"), FareClass.ECONOMY,
                Instant.parse("2026-10-23T10:00:00Z"));
        ReflectionTestUtils.setField(b, "id", ID);
        b.confirm("tx-1", Instant.now());
        b.requestCancel(Instant.now());
        when(service.cancelBooking(ID)).thenReturn(b);

        mvc.perform(post("/api/bookings/" + ID + "/cancel"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CANCELLING"));
    }

    /**
     * WHAT:   Cancelling a booking that cannot be cancelled now.
     * WHY:    409 with the service's reason.
     * RETURN: nothing; fails unless 409 with the message
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void conflictIs409() throws Exception
    {
        when(service.cancelBooking(ID)).thenThrow(new ConflictException("booking was never paid, so there is nothing to cancel"));
        mvc.perform(post("/api/bookings/" + ID + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("booking was never paid, so there is nothing to cancel"));
    }
}
