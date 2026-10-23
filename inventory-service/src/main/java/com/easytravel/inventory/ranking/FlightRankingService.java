package com.easytravel.inventory.ranking;

import com.easytravel.common.dto.FlightDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * WHAT:   Ranks flights by a tunable blend of price and duration ("best flights").
 * WHY:    Price (dollars) and duration (minutes) are different units, so
 *         adding them directly would be meaningless. Each is min-max
 *         normalised to 0..1 within the current results, then the two are
 *         combined with weights:
 *
 *             score = priceWeight * normalisedPrice + durationWeight * normalisedDuration
 *
 *         A lower score is better. The weights come from configuration, so
 *         "cheapest first" or "fastest first" is a setting, not a code change.
 *
 * @author AoceanP
 */
@Service
public class FlightRankingService
{
    /** WHAT: how much price matters, from application.yml (default 0.6). */
    private final double priceWeight;

    /** WHAT: how much duration matters, from application.yml (default 0.4). */
    private final double durationWeight;

    /**
     * WHAT:   Creates the ranker with its weights.
     * WHY:    Refuses nonsense weights at startup instead of producing odd rankings later.
     * PRE:    both weights are 0 or more, and at least one is above 0
     * THROWS: IllegalArgumentException for invalid weights
     *
     * @param priceWeight    how much price matters
     * @param durationWeight how much duration matters
     * @throws IllegalArgumentException if a weight is negative or both are 0
     */
    public FlightRankingService(
            @Value("${inventory.ranking.price-weight:0.6}") double priceWeight,
            @Value("${inventory.ranking.duration-weight:0.4}") double durationWeight)
    {
        if (priceWeight < 0 || durationWeight < 0 || priceWeight + durationWeight == 0)
        {
            throw new IllegalArgumentException("ranking weights must be 0 or more, and not both 0");
        }
        this.priceWeight = priceWeight;
        this.durationWeight = durationWeight;
    }

    /**
     * WHAT:   Sorts flights from best to worst.
     * WHY:    The sort is stable, so flights with equal scores keep the
     *         provider's order, and results never shuffle between requests.
     * PRE:    flights is not null
     * POST:   the input list is not changed
     * RETURN: a new list, best first
     *
     * @param flights the flights to rank
     * @return the ranked flights
     */
    public List<FlightDTO> rankBestFlights(List<FlightDTO> flights)
    {
        if (flights.size() <= 1)
        {
            return flights;
        }

        // 1. the range of each value across this result set, for normalisation
        double minPrice = flights.stream().mapToDouble(f -> f.price().doubleValue()).min().orElse(0);
        double maxPrice = flights.stream().mapToDouble(f -> f.price().doubleValue()).max().orElse(0);
        long   minDur   = flights.stream().mapToLong(FlightDTO::durationMinutes).min().orElse(0);
        long   maxDur   = flights.stream().mapToLong(FlightDTO::durationMinutes).max().orElse(0);

        double priceRange = maxPrice - minPrice;
        double durRange   = maxDur - minDur;

        // 2. sort by the weighted score, lowest (best) first
        return flights.stream()
                .sorted(Comparator.comparingDouble(f -> score(f, minPrice, priceRange, minDur, durRange)))
                .toList();
    }

    /**
     * WHAT:   One flight's score: 0 is the cheapest and fastest possible.
     * WHY:    A range of 0 (every flight the same price, say) would divide by
     *         zero, so that factor counts as 0 for everyone instead.
     * PRE:    the ranges were computed from the same flight list
     * RETURN: the weighted score
     *
     * @param f          the flight
     * @param minPrice   the lowest price in the list
     * @param priceRange highest minus lowest price
     * @param minDur     the shortest duration in the list
     * @param durRange   longest minus shortest duration
     * @return the score
     */
    private double score(FlightDTO f, double minPrice, double priceRange, long minDur, double durRange)
    {
        double normPrice = priceRange == 0 ? 0 : (f.price().doubleValue() - minPrice) / priceRange;
        double normDur   = durRange == 0 ? 0 : (f.durationMinutes() - minDur) / durRange;
        return priceWeight * normPrice + durationWeight * normDur;
    }
}
