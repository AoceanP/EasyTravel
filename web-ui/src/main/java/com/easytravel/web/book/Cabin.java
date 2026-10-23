package com.easytravel.web.book;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * WHAT:   The three cabins, with booking-service's price multipliers.
 * WHY:    The page shows each card's price before booking, using the same
 *         rule as the server (base fare × multiplier, rounded half-up to cents).
 *         The server still sets the real price; the ticket shows that one.
 *
 * @author AoceanP
 */
public enum Cabin
{
    ECONOMY("Economy", "1.0", 21),
    BUSINESS("Business", "2.5", 6),
    FIRST("First", "5.0", 1);

    /** WHAT: the name shown on the page. */
    private final String label;

    /** WHAT: the price multiplier, as in FareClass. */
    private final BigDecimal multiplier;

    /** WHAT: the first seat row number in this cabin. */
    private final int firstRow;

    /**
     * WHAT:   Creates a cabin.
     * PRE:    multiplier is a decimal number
     *
     * @param label      shown name
     * @param multiplier price multiplier
     * @param firstRow   first seat row
     */
    Cabin(String label, String multiplier, int firstRow)
    {
        this.label = label;
        this.multiplier = new BigDecimal(multiplier);
        this.firstRow = firstRow;
    }

    /**
     * WHAT:   The price of a base fare in this cabin.
     * PRE:    base is not null
     * RETURN: base × multiplier, rounded half-up to 2 decimals
     *
     * @param base the economy fare
     * @return the price
     */
    public BigDecimal price(BigDecimal base)
    {
        return base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * WHAT:   The name shown on the page.
     * PRE:    none
     * RETURN: e.g. "Business"
     *
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * WHAT:   The first seat row in this cabin.
     * PRE:    none
     * RETURN: the row number
     *
     * @return the first row
     */
    public int getFirstRow()
    {
        return firstRow;
    }
}
