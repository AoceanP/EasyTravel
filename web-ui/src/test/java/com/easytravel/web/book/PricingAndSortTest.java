package com.easytravel.web.book;

import com.easytravel.web.gateway.FlightDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WHAT:   Tests for cabin prices, money formatting and sorting.
 * WHY:    The page's card prices must match what booking-service will charge.
 *
 * @author AoceanP
 */
class PricingAndSortTest
{
    /** WHAT: three demo flights in the server's ranked order. */
    private final List<FlightDto> ranked = List.of(
            new FlightDto("DL215", "LHR", "JFK", new BigDecimal("240.00"), 660),
            new FlightDto("BA249", "LHR", "JFK", new BigDecimal("210.00"), 870),
            new FlightDto("UA930", "LHR", "JFK", new BigDecimal("420.00"), 390));

    /**
     * WHAT:   Prices per cabin, as booking-service computes them.
     * RETURN: nothing; fails if a price differs
     */
    @Test
    void cabinPricesMatchTheServer()
    {
        assertThat(Cabin.ECONOMY.price(new BigDecimal("210.00"))).isEqualByComparingTo("210.00");
        assertThat(Cabin.BUSINESS.price(new BigDecimal("355.00"))).isEqualByComparingTo("887.50");
        assertThat(Cabin.FIRST.price(new BigDecimal("420.00"))).isEqualByComparingTo("2100.00");
    }

    /**
     * WHAT:   Money and duration formatting.
     * RETURN: nothing; fails if the text differs
     */
    @Test
    void formatting()
    {
        assertThat(Money.format(new BigDecimal("2100"))).isEqualTo("2,100.00");
        assertThat(Money.format(null)).isEmpty();
        assertThat(Money.duration(665)).isEqualTo("11h 05m");
    }

    /**
     * WHAT:   The three orders.
     * WHY:    BEST must keep the server's ranking untouched.
     * RETURN: nothing; fails if an order is wrong
     */
    @Test
    void sortOrders()
    {
        assertThat(SortOrder.BEST.apply(ranked)).extracting(FlightDto::flightNumber).containsExactly("DL215", "BA249", "UA930");
        assertThat(SortOrder.CHEAPEST.apply(ranked)).extracting(FlightDto::flightNumber).containsExactly("BA249", "DL215", "UA930");
        assertThat(SortOrder.FASTEST.apply(ranked)).extracting(FlightDto::flightNumber).containsExactly("UA930", "DL215", "BA249");
    }

    /**
     * WHAT:   Airline names and styles.
     * RETURN: nothing; fails if a lookup is wrong
     */
    @Test
    void airlines()
    {
        assertThat(Airlines.name("BA249")).isEqualTo("British Airways");
        assertThat(Airlines.css("vs045")).isEqualTo("al-vs");
        assertThat(Airlines.name("ZZ1")).isEqualTo("ZZ");
        assertThat(Airlines.css("ZZ1")).isEqualTo("al-other");
    }
}
