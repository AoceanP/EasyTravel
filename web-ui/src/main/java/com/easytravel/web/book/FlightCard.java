package com.easytravel.web.book;

/**
 * WHAT:   Everything one flight card on the Book page shows.
 * WHY:    Computed once in the page bean, so the template only reads values.
 *
 * @param code      flight number
 * @param airline   airline name
 * @param mono      2-letter monogram
 * @param monoCss   monogram colour class
 * @param price     price in the chosen cabin, formatted
 * @param duration  flight time, formatted
 * @param barWidth  CSS width of the duration bar, e.g. "76%"
 * @param tag       tag text for the first card, or null
 * @param note      text under the price
 * @param noteCss   class for that text (warns when over the card limit)
 * @param css       card classes (selected or not)
 * @param selected  whether it is the chosen flight
 * @author AoceanP
 */
public record FlightCard(String code, String airline, String mono, String monoCss, String price, String duration,
                         String barWidth, String tag, String note, String noteCss, String css, boolean selected)
{
}
