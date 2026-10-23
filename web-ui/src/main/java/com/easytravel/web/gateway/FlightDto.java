package com.easytravel.web.gateway;

import java.math.BigDecimal;

/**
 * WHAT:   One flight as the inventory service returns it through the gateway.
 * WHY:    The website's own copy of the JSON shape, so this WAR does not depend
 *         on the Spring-based common module.
 *
 * @param flightNumber    e.g. "BA249"
 * @param origin          IATA code, e.g. "LHR"
 * @param destination     IATA code, e.g. "JFK"
 * @param price           base (economy) fare
 * @param durationMinutes flight time
 * @author AoceanP
 */
public record FlightDto(String flightNumber, String origin, String destination, BigDecimal price, int durationMinutes)
{
}
