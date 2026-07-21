package com.easytravel.inventory.provider;

import com.easytravel.common.dto.FlightDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * Stand-in for a real third-party flight API (e.g. Amadeus). Returns a deterministic
 * candidate set with a real price/duration spread so ranking has a genuine decision.
 * Durations are given directly in minutes (14h30 = 870, etc.).
 */
@Component
public class MockFlightProvider implements FlightProvider {

    @Override
    public List<FlightDTO> searchFlights(String origin, String destination) {
        return List.of(
                new FlightDTO("BA249", origin, destination, new BigDecimal("210.00"), 870),   // 14h30
                new FlightDTO("VS045", origin, destination, new BigDecimal("355.00"), 465),   // 7h45
                new FlightDTO("AA101", origin, destination, new BigDecimal("280.00"), 555),   // 9h15
                new FlightDTO("DL215", origin, destination, new BigDecimal("240.00"), 660),   // 11h00
                new FlightDTO("UA930", origin, destination, new BigDecimal("420.00"), 390)    // 6h30
        );
    }
}