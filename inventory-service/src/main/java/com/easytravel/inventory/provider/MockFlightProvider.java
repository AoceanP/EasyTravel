package com.easytravel.inventory.provider;

import com.easytravel.common.dto.FlightDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * WHAT:   A stand-in for a real flight API, returning the same five flights
 *         for any route.
 * WHY:    Deterministic data with a real spread of prices and durations, so
 *         the ranking has a genuine trade-off to make and tests always see
 *         the same flights. All data is fake.
 *
 * @author AoceanP
 */
@Component
public class MockFlightProvider implements FlightProvider
{
    /**
     * WHAT:   Returns the five demo flights on the requested route.
     * WHY:    Cheapest is slowest and fastest is most expensive, so
     *         "best" depends on the ranking weights.
     * PRE:    none
     * RETURN: five flights
     *
     * @param origin      the departure airport
     * @param destination the arrival airport
     * @return the flights
     */
    @Override
    public List<FlightDTO> searchFlights(String origin, String destination)
    {
        return List.of(
                new FlightDTO("BA249", origin, destination, new BigDecimal("210.00"), 870),   // 14h30, cheapest
                new FlightDTO("VS045", origin, destination, new BigDecimal("355.00"), 465),   // 7h45
                new FlightDTO("AA101", origin, destination, new BigDecimal("280.00"), 555),   // 9h15
                new FlightDTO("DL215", origin, destination, new BigDecimal("240.00"), 660),   // 11h00
                new FlightDTO("UA930", origin, destination, new BigDecimal("420.00"), 390)    // 6h30, fastest
        );
    }
}
