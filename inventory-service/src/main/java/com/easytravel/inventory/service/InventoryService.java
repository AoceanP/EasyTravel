package com.easytravel.inventory.service;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.inventory.provider.FlightProvider;
import com.easytravel.inventory.ranking.FlightRankingService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates "find the best flights": fetch candidates from the provider, hand them
 * to the ranking service. Thin coordinating layer; the real work lives in its two deps.
 * Constructor injection keeps fields final and makes the class trivially unit-testable.
 */
@Service
public class InventoryService {

    private final FlightProvider flightProvider;
    private final FlightRankingService rankingService;

    public InventoryService(FlightProvider flightProvider,
                            FlightRankingService rankingService) {
        this.flightProvider = flightProvider;
        this.rankingService = rankingService;
    }

    public List<FlightDTO> findBestFlights(String origin, String destination) {
        List<FlightDTO> candidates = flightProvider.searchFlights(origin, destination);
        return rankingService.rankBestFlights(candidates);
    }
}