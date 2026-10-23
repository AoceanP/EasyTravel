package com.easytravel.web.trips;

/**
 * WHAT:   Everything one trip row on the My trips page shows.
 *
 * @param id         the booking id
 * @param shortId    its first 8 characters
 * @param code       flight number
 * @param airline    airline name
 * @param mono       2-letter monogram
 * @param monoCss    monogram colour class
 * @param cabin      cabin name
 * @param price      amount paid, formatted
 * @param booked     when it was booked, formatted
 * @param status     the status pill's text
 * @param pillCss    the pill's classes
 * @param note       a line under the details (decline or refund reason), or null
 * @param noteCss    classes for that line
 * @param css        row classes (selected or not)
 * @param selected   whether its pass is showing
 * @param canCancel  whether "Cancel trip" is offered
 * @param confirming whether the "are you sure" question is open
 * @author AoceanP
 */
public record TripCard(String id, String shortId, String code, String airline, String mono, String monoCss,
                       String cabin, String price, String booked, String status, String pillCss, String note,
                       String noteCss, String css, boolean selected, boolean canCancel, boolean confirming)
{
}
