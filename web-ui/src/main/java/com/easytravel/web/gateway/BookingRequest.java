package com.easytravel.web.gateway;

/**
 * WHAT:   The body of POST /api/bookings.
 * WHY:    No price field, on purpose: the server sets the price.
 *
 * @param userId       who is booking
 * @param flightNumber the flight
 * @param origin       IATA code
 * @param destination  IATA code
 * @param fareClass    ECONOMY, BUSINESS or FIRST
 * @author AoceanP
 */
public record BookingRequest(String userId, String flightNumber, String origin, String destination, String fareClass)
{
}
