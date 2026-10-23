package com.easytravel.inventory.ranking;

import com.easytravel.common.dto.FlightDTO;
import com.easytravel.inventory.provider.MockFlightProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WHAT:   Tests for the "best flights" ranking.
 * WHY:    The ranking is the inventory service's real logic; each test
 *         checks an order worked out by hand from the formula.
 *
 * @author AoceanP
 */
class FlightRankingServiceTest
{
    /** WHAT: the five demo flights on a route. */
    private final List<FlightDTO> flights = new MockFlightProvider().searchFlights("LHR", "JFK");

    /**
     * WHAT:   The default weights, 60% price and 40% duration.
     * WHY:    Worked out by hand: DL215 scores 0.311, AA101 0.338, BA249
     *         0.400, VS045 0.477, UA930 0.600. Neither the cheapest nor the
     *         fastest flight wins: that is the point of a blended score.
     * PRE:    none
     * RETURN: nothing; fails if the order differs
     */
    @Test
    void defaultWeightsBalancePriceAndDuration()
    {
        assertThat(new FlightRankingService(0.6, 0.4).rankBestFlights(flights))
                .extracting(FlightDTO::flightNumber)
                .containsExactly("DL215", "AA101", "BA249", "VS045", "UA930");
    }

    /**
     * WHAT:   Price only.
     * WHY:    Must sort from cheapest to most expensive.
     * PRE:    none
     * RETURN: nothing; fails if the order is not by price
     */
    @Test
    void priceOnlyIsCheapestFirst()
    {
        assertThat(new FlightRankingService(1, 0).rankBestFlights(flights))
                .extracting(FlightDTO::flightNumber)
                .containsExactly("BA249", "DL215", "AA101", "VS045", "UA930");
    }

    /**
     * WHAT:   Duration only.
     * WHY:    Must sort from fastest to slowest.
     * PRE:    none
     * RETURN: nothing; fails if the order is not by duration
     */
    @Test
    void durationOnlyIsFastestFirst()
    {
        assertThat(new FlightRankingService(0, 1).rankBestFlights(flights))
                .extracting(FlightDTO::flightNumber)
                .containsExactly("UA930", "VS045", "AA101", "DL215", "BA249");
    }

    /**
     * WHAT:   Flights that all cost and take the same.
     * WHY:    Both ranges are 0; dividing by them would give NaN. The order
     *         must simply stay as it came (the sort is stable).
     * PRE:    none
     * RETURN: nothing; fails if the order changes or anything breaks
     */
    @Test
    void identicalFlightsKeepTheirOrder()
    {
        List<FlightDTO> same = List.of(
                new FlightDTO("X1", "LHR", "JFK", new BigDecimal("100.00"), 400),
                new FlightDTO("X2", "LHR", "JFK", new BigDecimal("100.00"), 400),
                new FlightDTO("X3", "LHR", "JFK", new BigDecimal("100.00"), 400));
        assertThat(new FlightRankingService(0.6, 0.4).rankBestFlights(same))
                .extracting(FlightDTO::flightNumber)
                .containsExactly("X1", "X2", "X3");
    }

    /**
     * WHAT:   No flights, and one flight.
     * WHY:    The smallest inputs must not break the min/max maths.
     * PRE:    none
     * RETURN: nothing; fails if the result is wrong
     */
    @Test
    void emptyAndSingleLists()
    {
        FlightRankingService ranking = new FlightRankingService(0.6, 0.4);
        assertThat(ranking.rankBestFlights(List.of())).isEmpty();
        assertThat(ranking.rankBestFlights(flights.subList(0, 1))).containsExactly(flights.get(0));
    }

    /**
     * WHAT:   Negative weights, and both weights 0.
     * WHY:    Refused at startup, instead of producing a meaningless ranking.
     * PRE:    none
     * RETURN: nothing; fails if they are accepted
     */
    @Test
    void invalidWeightsAreRefused()
    {
        assertThatThrownBy(() -> new FlightRankingService(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FlightRankingService(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
