package com.easytravel.inventory.ranking;

import com.easytravel.common.dto.FlightDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * @author alekaandar panich
 * @version 1.0
 *
 * Ranks flights by a tunable blend of price and duration ("Best Flights").
 *
 * Price (dollars) and duration (minutes) are incomparable units, so we min-max
 * normalize each to 0..1 within the current set, then combine with weights.
 * Lower score = better.  score = priceWeight * normPrice + durationWeight * normDur
 */
@Service
public class FlightRankingService
{

    private final double priceWeight;
    private final double durationWeight;

    public FlightRankingService(
            @Value("${inventory.ranking.price-weight:0.6}") double priceWeight,
            @Value("${inventory.ranking.duration-weight:0.4}") double durationWeight)
    {
        this.priceWeight = priceWeight;
        this.durationWeight = durationWeight;
    }

    public List<FlightDTO> rankBestFlights(List<FlightDTO> flights)
    {
        if (flights.size() <= 1)
        {
            return flights;
        }

        double minPrice = flights.stream().mapToDouble(f -> f.price().doubleValue()).min().orElse(0);
        double maxPrice = flights.stream().mapToDouble(f -> f.price().doubleValue()).max().orElse(0);
        long   minDur   = flights.stream().mapToLong(FlightDTO::durationMinutes).min().orElse(0);
        long   maxDur   = flights.stream().mapToLong(FlightDTO::durationMinutes).max().orElse(0);

        double priceRange = maxPrice - minPrice;
        double durRange   = maxDur - minDur;

        return flights.stream()
                .sorted(Comparator.comparingDouble(f ->
                        score(f, minPrice, priceRange, minDur, durRange)))
                .toList();
    }

    private double score(FlightDTO f,
                         double minPrice, double priceRange,
                         long minDur, double durRange)
    {
        double normPrice = priceRange == 0 ? 0
                : (f.price().doubleValue() - minPrice) / priceRange;
        double normDur = durRange == 0 ? 0
                : (f.durationMinutes() - minDur) / durRange;

        return priceWeight * normPrice + durationWeight * normDur;
    }
}