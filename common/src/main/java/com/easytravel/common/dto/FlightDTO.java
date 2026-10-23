package com.easytravel.common.dto;

import java.math.BigDecimal;

/**
 * WHAT:   One bookable flight option.
 * WHY:    Duration is a plain number of minutes rather than a
 *         java.time.Duration: it serialises to clean JSON (just a number)
 *         and is trivially comparable for the ranking algorithm.
 *         Money is always BigDecimal, never double, so 0.1 + 0.2 is exactly 0.3.
 *
 * @param flightNumber    carrier and number, e.g. "BA249"
 * @param origin          IATA departure code, e.g. "LHR"
 * @param destination     IATA arrival code, e.g. "JFK"
 * @param price           the base (economy) fare
 * @param durationMinutes total travel time in minutes
 * @author AoceanP
 */
public record FlightDTO(
        String flightNumber,
        String origin,
        String destination,
        BigDecimal price,
        long durationMinutes
)
{
}
