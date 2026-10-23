package com.easytravel.inventory.controller;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.inventory.service.InventoryService;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WHAT:   The HTTP edge of the inventory service.
 * WHY:    Stays thin: read the request, call the service, return JSON. No
 *         business logic here, so the logic can be tested without HTTP.
 *
 *         GET /api/flights/search?origin=LHR&amp;destination=JFK
 *         GET /api/flights/quote?flightNumber=BA249&amp;origin=LHR&amp;destination=JFK
 *
 * @author AoceanP
 */
@RestController
@Validated
public class FlightSearchController
{
    /** WHAT: does the actual work. */
    private final InventoryService inventoryService;

    /**
     * WHAT:   Creates the controller.
     * PRE:    inventoryService is not null
     *
     * @param inventoryService the service to delegate to
     */
    public FlightSearchController(InventoryService inventoryService)
    {
        this.inventoryService = inventoryService;
    }

    /**
     * WHAT:   Searches a route and returns the flights, best first.
     * PRE:    origin and destination are 3 capital letters
     * RETURN: the ranked flights as JSON
     *
     * @param origin      the departure airport, e.g. "LHR"
     * @param destination the arrival airport, e.g. "JFK"
     * @return the flights
     */
    @GetMapping("/api/flights/search")
    public List<FlightDTO> searchFlights(
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String origin,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String destination)
    {
        return inventoryService.findBestFlights(origin, destination);
    }

    /**
     * WHAT:   Returns one flight with its current base fare.
     * WHY:    Used by booking-service to price a booking server-side.
     * PRE:    origin and destination are 3 capital letters
     * RETURN: the flight as JSON, or 404 if it does not exist
     *
     * @param flightNumber the flight, e.g. "BA249"
     * @param origin       the departure airport
     * @param destination  the arrival airport
     * @return the flight
     */
    @GetMapping("/api/flights/quote")
    public FlightDTO quote(
            @RequestParam String flightNumber,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String origin,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String destination)
    {
        return inventoryService.quote(flightNumber, origin, destination);
    }
}
