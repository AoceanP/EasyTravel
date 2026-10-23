package com.easytravel.inventory.provider;

import com.easytravel.common.dto.FlightDTO;

import java.util.List;

/**
 * WHAT:   Where flight data comes from.
 * WHY:    The service depends on this interface, not on a concrete source,
 *         so a real airline API (e.g. Amadeus) can replace the mock without
 *         touching anything else (the dependency inversion principle).
 *
 * @author AoceanP
 */
public interface FlightProvider
{
    /**
     * WHAT:   Lists the flights on a route.
     * PRE:    origin and destination are 3-letter IATA codes
     * RETURN: the flights, in no particular order; empty if there are none
     *
     * @param origin      the departure airport
     * @param destination the arrival airport
     * @return the flights
     */
    List<FlightDTO> searchFlights(String origin, String destination);
}
