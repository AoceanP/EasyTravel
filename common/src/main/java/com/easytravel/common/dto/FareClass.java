package com.easytravel.common.dto;

import java.math.BigDecimal;

/**
 * WHAT:   The cabin a customer books, and how it changes the price.
 * WHY:    The price multiplier lives on the enum itself, so adding a cabin
 *         means adding one line here instead of hunting for if-statements.
 *         Lives in common because it is part of the public booking request.
 *
 * @author AoceanP
 */
public enum FareClass
{
    /** WHAT: the base fare. */
    ECONOMY(new BigDecimal("1.0")),

    /** WHAT: 2.5 times the base fare. */
    BUSINESS(new BigDecimal("2.5")),

    /** WHAT: 5 times the base fare. */
    FIRST(new BigDecimal("5.0"));

    /** WHAT: what the base fare is multiplied by for this cabin. */
    private final BigDecimal multiplier;

    /**
     * WHAT:   Creates a cabin with its price multiplier.
     * PRE:    multiplier is positive
     *
     * @param multiplier what the base fare is multiplied by
     */
    FareClass(BigDecimal multiplier)
    {
        this.multiplier = multiplier;
    }

    /**
     * WHAT:   Returns the price multiplier.
     * WHY:    The booking service turns a flight's base fare into the price
     *         the customer pays.
     * PRE:    none
     * RETURN: the multiplier, e.g. 2.5 for BUSINESS
     *
     * @return the multiplier
     */
    public BigDecimal multiplier()
    {
        return multiplier;
    }
}
