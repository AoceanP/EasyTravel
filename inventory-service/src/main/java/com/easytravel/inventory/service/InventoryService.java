package com.easytravel.inventory.service;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.common.exception.ResourceNotFoundException;
import com.easytravel.inventory.provider.FlightProvider;
import com.easytravel.inventory.ranking.FlightRankingService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WHAT:   Searches flights and quotes the price of one flight.
 * WHY:    A thin coordinating layer: the provider supplies flights, the
 *         ranking service orders them. Constructor injection keeps the
 *         fields final and the class easy to unit test with fakes.
 *
 * @author AoceanP
 */
@Service
public class InventoryService
{
    /** WHAT: where flights come from. */
    private final FlightProvider flightProvider;

    /** WHAT: orders flights from best to worst. */
    private final FlightRankingService rankingService;

    /**
     * WHAT:   Creates the service with its two collaborators.
     * PRE:    both are not null (Spring injects them)
     *
     * @param flightProvider where flights come from
     * @param rankingService how flights are ranked
     */
    public InventoryService(FlightProvider flightProvider, FlightRankingService rankingService)
    {
        this.flightProvider = flightProvider;
        this.rankingService = rankingService;
    }

    /**
     * WHAT:   Returns the flights on a route, best first.
     * PRE:    origin and destination are 3-letter IATA codes
     * RETURN: the ranked flights
     *
     * @param origin      the departure airport
     * @param destination the arrival airport
     * @return the ranked flights
     */
    public List<FlightDTO> findBestFlights(String origin, String destination)
    {
        return rankingService.rankBestFlights(flightProvider.searchFlights(origin, destination));
    }

    /**
     * WHAT:   Returns one flight, with its current price.
     * WHY:    booking-service calls this to get the real fare, so a client
     *         can never choose its own price.
     * PRE:    flightNumber, origin and destination are not null
     * RETURN: the flight
     * THROWS: ResourceNotFoundException if no such flight flies that route
     *
     * @param flightNumber the flight, e.g. "BA249"
     * @param origin       the departure airport
     * @param destination  the arrival airport
     * @return the flight
     * @throws ResourceNotFoundException if the flight is not found
     */
    public FlightDTO quote(String flightNumber, String origin, String destination)
    {
        return flightProvider.searchFlights(origin, destination).stream()
                .filter(flight -> flight.flightNumber().equalsIgnoreCase(flightNumber))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "flight " + flightNumber + " from " + origin + " to " + destination + " not found"));
    }
}
