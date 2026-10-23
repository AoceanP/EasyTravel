package com.easytravel.web.book;

import com.easytravel.web.gateway.FlightDto;

import java.util.Comparator;
import java.util.List;

/**
 * WHAT:   How the flight list is ordered.
 * WHY:    BEST keeps inventory-service's own ranking (its price/duration
 *         blend); the other two are simple re-sorts done on the page.
 *
 * @author AoceanP
 */
public enum SortOrder
{
    BEST("Best", "Best balance"),
    CHEAPEST("Cheapest", "Cheapest"),
    FASTEST("Fastest", "Fastest");

    /** WHAT: the button text. */
    private final String label;

    /** WHAT: the tag shown on the first flight. */
    private final String tag;

    /**
     * WHAT:   Creates an order.
     * PRE:    none
     *
     * @param label button text
     * @param tag   first flight's tag
     */
    SortOrder(String label, String tag)
    {
        this.label = label;
        this.tag = tag;
    }

    /**
     * WHAT:   Orders flights.
     * PRE:    flights is in the server's ranked order
     * RETURN: a new list in this order; ties keep the server's order
     *
     * @param flights the flights
     * @return the ordered flights
     */
    public List<FlightDto> apply(List<FlightDto> flights)
    {
        return switch (this)
        {
            case BEST -> List.copyOf(flights);
            case CHEAPEST -> flights.stream().sorted(Comparator.comparing(FlightDto::price)).toList();
            case FASTEST -> flights.stream().sorted(Comparator.comparingInt(FlightDto::durationMinutes)).toList();
        };
    }

    /**
     * WHAT:   The button text.
     * PRE:    none
     * RETURN: e.g. "Cheapest"
     *
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * WHAT:   The tag for the first flight in this order.
     * PRE:    none
     * RETURN: e.g. "Best balance"
     *
     * @return the tag
     */
    public String getTag()
    {
        return tag;
    }
}
