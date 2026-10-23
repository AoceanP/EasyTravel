package com.easytravel.web.trips;

import java.util.ArrayList;
import java.util.List;

/**
 * WHAT:   A decorative barcode drawn from a booking id: alternating dark bars
 *         and gaps of varying width.
 * WHY:    Makes each boarding pass look unique and real without an image or a
 *         barcode library. It encodes nothing and is not scannable; the booking
 *         reference printed next to it is the real identifier.
 *
 * @author AoceanP
 */
public final class Barcode
{
    /** WHAT: how many bars and gaps in total. */
    static final int ELEMENTS = 44;

    /**
     * WHAT:   One bar or gap.
     *
     * @param width CSS width, e.g. "3px"
     * @param css   "bar-dark" or "bar-gap"
     */
    public record Bar(String width, String css)
    {
    }

    /**
     * WHAT:   Not instantiable.
     * WHY:    Static builder only.
     */
    private Barcode()
    {
    }

    /**
     * WHAT:   Builds the bars for a booking id.
     * PRE:    seed is not empty
     * RETURN: ELEMENTS bars, starting and ending dark; the same seed always gives the same bars
     *
     * @param seed the booking id
     * @return the bars
     */
    public static List<Bar> of(String seed)
    {
        List<Bar> bars = new ArrayList<>(ELEMENTS);
        for (int i = 0; i < ELEMENTS; i++)
        {
            int width = 1 + Math.floorMod(seed.charAt(i % seed.length()) * 31 + i * 7, 3);
            bars.add(new Bar(width + "px", i % 2 == 0 ? "bar-dark" : "bar-gap"));
        }
        // an even count would end on a gap; make the last element dark
        bars.set(ELEMENTS - 1, new Bar(bars.get(ELEMENTS - 1).width(), "bar-dark"));
        return bars;
    }
}
