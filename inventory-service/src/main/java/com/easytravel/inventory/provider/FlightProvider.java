package com.easytravel.inventory.provider;

import com.easytravel.common.dto.FlightDTO;
import java.util.List;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * Abstraction over an external flight source. The service depends on THIS, not on any
 * concrete provider — so a real Amadeus client can replace the mock with no ripple.
 */
public interface FlightProvider {
    List<FlightDTO> searchFlights(String origin, String destination);
}