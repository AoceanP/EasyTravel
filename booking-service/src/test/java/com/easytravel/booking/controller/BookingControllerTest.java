package com.easytravel.booking.controller;

import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.service.BookingService;
import com.easytravel.common.dto.BookingRequestDTO;
import com.easytravel.common.dto.FareClass;
import com.easytravel.common.exception.GlobalExceptionHandler;
import com.easytravel.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WHAT:   Tests for the booking HTTP endpoints: status codes, validation and error bodies.
 * WHY:    The HTTP contract is what clients depend on; standalone MockMvc
 *         tests it with the real validator and error handler, no server needed.
 *
 * @author AoceanP
 */
class BookingControllerTest
{
    /** WHAT: the fake service behind the controller. */
    private BookingService service;

    /** WHAT: Spring MVC driven in memory. */
    private MockMvc mvc;

    /**
     * WHAT:   Wires the controller with a fake service, the real validator and the shared error handler.
     * POST:   mvc is ready
     */
    @BeforeEach
    void setUp()
    {
        service = mock(BookingService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new BookingController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * WHAT:   A valid booking request.
     * WHY:    201 Created, a Location header for polling, and the PENDING booking.
     * PRE:    none
     * RETURN: nothing; fails if the response is wrong
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void createReturns201WithLocation() throws Exception
    {
        Booking booking = new Booking("alice", "BA249", "LHR", "JFK", new BigDecimal("525.00"), FareClass.BUSINESS,
                Instant.parse("2026-10-21T10:00:00Z"));
        ReflectionTestUtils.setField(booking, "id", UUID.fromString("00000000-0000-0000-0000-000000000007"));
        when(service.createBooking(any(BookingRequestDTO.class))).thenReturn(booking);

        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":"alice","flightNumber":"BA249","origin":"LHR","destination":"JFK","fareClass":"BUSINESS"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/bookings/00000000-0000-0000-0000-000000000007"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.price").value(525.00));
    }

    /**
     * WHAT:   A request that tries to set its own price.
     * WHY:    The old API trusted "amount" from the client; now the field does
     *         not exist, so it is ignored and the server prices the booking.
     * PRE:    none
     * RETURN: nothing; fails if the client's price reaches the service
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void clientPriceIsIgnored() throws Exception
    {
        Booking booking = new Booking("mallory", "BA249", "LHR", "JFK", new BigDecimal("210.00"), FareClass.ECONOMY,
                Instant.parse("2026-10-21T10:00:00Z"));
        ReflectionTestUtils.setField(booking, "id", UUID.randomUUID());
        when(service.createBooking(any(BookingRequestDTO.class))).thenReturn(booking);

        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":"mallory","flightNumber":"BA249","origin":"LHR","destination":"JFK","fareClass":"ECONOMY","amount":1.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(210.00));
    }

    /**
     * WHAT:   A request with every field wrong at once.
     * WHY:    One 400 listing every problem, and the service is never called.
     * PRE:    none
     * RETURN: nothing; fails unless all four fields are reported
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void invalidRequestListsEveryField() throws Exception
    {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":" ","flightNumber":"","origin":"lhr","destination":"JFK"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userId").value("userId must not be blank"))
                .andExpect(jsonPath("$.details.flightNumber").exists())
                .andExpect(jsonPath("$.details.origin").value("origin must be a 3-letter airport code, e.g. LHR"))
                .andExpect(jsonPath("$.details.fareClass").exists());
        verify(service, never()).createBooking(any());
    }

    /**
     * WHAT:   An unknown cabin and a body that is not JSON.
     * WHY:    Both are client mistakes: 400, never 500.
     * PRE:    none
     * RETURN: nothing; fails unless both are 400
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void unreadableBodiesAre400() throws Exception
    {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("""
                        {"userId":"alice","flightNumber":"BA249","origin":"LHR","destination":"JFK","fareClass":"PREMIUM"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    /**
     * WHAT:   A booking that does not exist, and an id that is not a UUID.
     * WHY:    404 and 400; the old API answered 500 for a missing booking.
     * PRE:    none
     * RETURN: nothing; fails if the status codes are wrong
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void missingAndMalformedIds() throws Exception
    {
        UUID missing = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(service.getBooking(missing)).thenThrow(new ResourceNotFoundException("booking " + missing + " not found"));
        mvc.perform(get("/api/bookings/" + missing))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("booking " + missing + " not found"));
        mvc.perform(get("/api/bookings/abc"))
                .andExpect(status().isBadRequest());
    }
}
