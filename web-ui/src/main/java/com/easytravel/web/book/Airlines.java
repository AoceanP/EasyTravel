package com.easytravel.web.book;

import java.util.Map;

/**
 * WHAT:   Airline names and colour styles for the demo flight numbers.
 * WHY:    The inventory API returns only flight numbers ("DL215"); the cards
 *         read better with the airline's name and a coloured monogram.
 *
 * @author AoceanP
 */
public final class Airlines
{
    /** WHAT: airline name by the flight number's 2-letter prefix. */
    private static final Map<String, String> NAMES = Map.of(
            "BA", "British Airways",
            "DL", "Delta",
            "AA", "American",
            "VS", "Virgin Atlantic",
            "UA", "United");

    /**
     * WHAT:   Not instantiable.
     * WHY:    Lookups only.
     */
    private Airlines()
    {
    }

    /**
     * WHAT:   The 2-letter airline code of a flight number.
     * PRE:    flightNumber is not null
     * RETURN: e.g. "BA" for "BA249"; the whole text if shorter than 2
     *
     * @param flightNumber the flight
     * @return the airline code
     */
    public static String code(String flightNumber)
    {
        return flightNumber.length() < 2 ? flightNumber : flightNumber.substring(0, 2).toUpperCase();
    }

    /**
     * WHAT:   The airline's name.
     * PRE:    flightNumber is not null
     * RETURN: e.g. "British Airways"; the code itself if unknown
     *
     * @param flightNumber the flight
     * @return the name
     */
    public static String name(String flightNumber)
    {
        String code = code(flightNumber);
        return NAMES.getOrDefault(code, code);
    }

    /**
     * WHAT:   The CSS class that colours the airline's monogram.
     * PRE:    flightNumber is not null
     * RETURN: e.g. "al-ba"; "al-other" if unknown
     *
     * @param flightNumber the flight
     * @return the class
     */
    public static String css(String flightNumber)
    {
        String code = code(flightNumber);
        return NAMES.containsKey(code) ? "al-" + code.toLowerCase() : "al-other";
    }
}
