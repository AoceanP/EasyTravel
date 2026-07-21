package com.easytravel.common.dto;

import java.math.BigDecimal;

/**
 * Immutable contract describing a single bookable flight option.
 *
 * <p>Duration is expressed as total minutes (a plain long) rather than a
 * java.time.Duration. Two reasons: (1) it serializes to clean, unambiguous JSON
 * (just a number) with no extra Jackson modules, and (2) minutes are trivially
 * comparable for the ranking algorithm. A consumer can format it however it likes.
 *
 * @param flightNumber    carrier + number, e.g. "BA249"
 * @param origin          IATA departure code, e.g. "LHR"
 * @param destination     IATA arrival code, e.g. "JFK"
 * @param price           fare amount; BigDecimal, never double, for money
 * @param durationMinutes total travel time in minutes; used by the ranking algorithm
 */
public record FlightDTO(
        String flightNumber,
        String origin,
        String destination,
        BigDecimal price,
        long durationMinutes
) {
}