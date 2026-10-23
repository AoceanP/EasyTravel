package com.easytravel.inventory.controller;

import com.easytravel.common.exception.GlobalExceptionHandler;
import com.easytravel.inventory.provider.MockFlightProvider;
import com.easytravel.inventory.ranking.FlightRankingService;
import com.easytravel.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WHAT:   Tests for the inventory HTTP endpoints, through Spring MVC.
 * WHY:    Standalone MockMvc runs the real controller, JSON conversion and
 *         error handler without starting a server or Eureka.
 *
 * @author AoceanP
 */
class FlightSearchControllerTest
{
    /** WHAT: Spring MVC driven in memory. */
    private MockMvc mvc;

    /**
     * WHAT:   Wires the real controller, service and error handler.
     * POST:   mvc is ready
     */
    @BeforeEach
    void setUp()
    {
        InventoryService service = new InventoryService(new MockFlightProvider(), new FlightRankingService(0.6, 0.4));
        mvc = MockMvcBuilders.standaloneSetup(new FlightSearchController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * WHAT:   A route search.
     * WHY:    Five flights come back as JSON, best first.
     * PRE:    none
     * RETURN: nothing; fails if the JSON is wrong
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void searchReturnsRankedFlights() throws Exception
    {
        mvc.perform(get("/api/flights/search").param("origin", "LHR").param("destination", "JFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].flightNumber").value("DL215"))
                .andExpect(jsonPath("$[0].price").value(240.00));
    }

    /**
     * WHAT:   A quote for a flight that exists, with the flight number in lowercase.
     * WHY:    booking-service relies on this for the real price; lookups ignore case.
     * PRE:    none
     * RETURN: nothing; fails if the price is wrong
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void quoteReturnsTheFlight() throws Exception
    {
        mvc.perform(get("/api/flights/quote").param("flightNumber", "ba249").param("origin", "LHR").param("destination", "JFK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("BA249"))
                .andExpect(jsonPath("$.price").value(210.00));
    }

    /**
     * WHAT:   A quote for a flight that does not exist.
     * WHY:    Must be a 404 in the shared error format, which booking-service
     *         turns into "flight not found" for its own client.
     * PRE:    none
     * RETURN: nothing; fails unless 404 with the standard error body
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void unknownFlightIs404() throws Exception
    {
        mvc.perform(get("/api/flights/quote").param("flightNumber", "ZZ999").param("origin", "LHR").param("destination", "JFK"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("flight ZZ999 from LHR to JFK not found"));
    }

    /**
     * WHAT:   A search with a missing parameter.
     * WHY:    Must be a client error, not a server error.
     * PRE:    none
     * RETURN: nothing; fails unless 400
     * THROWS: Exception if MockMvc fails
     *
     * @throws Exception if the request cannot be performed
     */
    @Test
    void missingParameterIs400() throws Exception
    {
        mvc.perform(get("/api/flights/search").param("origin", "LHR"))
                .andExpect(status().isBadRequest());
    }
}
