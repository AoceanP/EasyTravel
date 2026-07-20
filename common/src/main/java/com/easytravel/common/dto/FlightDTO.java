package com.easytravel.common.dto;

import java.math.BigDecimal;
import java.time.Duration;

/**
 *
 * @author aleksandar panich
 * @version 1.0
 *
 * Immutable contract describing a single bookable flight option.
 *
 * <p>This is an OUTPUT/data-carrying DTO: inventory-service produces it (from the
 * mocked provider) and other services / clients consume it. Being a record makes it
 * immutable and value-based — exactly what a data contract crossing service
 * boundaries should be.
 *
 * @param flightNumber carrier + number, e.g. "BA249"
 * @param origin       IATA departure code, e.g. "LHR"
 * @param destination  IATA arrival code, e.g. "JFK"
 * @param price        fare amount; BigDecimal, never double, for money
 * @param duration     total travel time, used by the "Best Flights" ranking in Phase 2.1
 */
public record FlightDTO(
        String flightNumber,
        String origin,
        String destination,
        BigDecimal price,
        Duration duration
)
{}