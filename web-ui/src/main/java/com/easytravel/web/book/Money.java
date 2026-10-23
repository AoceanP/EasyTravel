package com.easytravel.web.book;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * WHAT:   Formats amounts the way the page shows them: "2,100.00".
 * WHY:    One place for the format, so cards, ticket and messages agree.
 *
 * @author AoceanP
 */
public final class Money
{
    /**
     * WHAT:   Not instantiable.
     * WHY:    Static helper.
     */
    private Money()
    {
    }

    /**
     * WHAT:   Formats an amount with thousands separators and 2 decimals.
     * PRE:    none
     * RETURN: the text, or "" for null
     *
     * @param amount the amount
     * @return e.g. "887.50"
     */
    public static String format(BigDecimal amount)
    {
        if (amount == null)
        {
            return "";
        }
        // DecimalFormat is not thread-safe, so a new one per call
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
    }

    /**
     * WHAT:   Formats a duration in minutes.
     * PRE:    minutes is not negative
     * RETURN: e.g. "11h 05m"
     *
     * @param minutes the duration
     * @return the text
     */
    public static String duration(int minutes)
    {
        return "%dh %02dm".formatted(minutes / 60, minutes % 60);
    }
}
