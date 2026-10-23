package com.easytravel.web.trips;

import java.util.List;

/**
 * WHAT:   The boarding pass for the selected trip.
 *
 * @param code     flight number
 * @param airline  airline name
 * @param cabin    cabin name, upper case
 * @param price    amount paid
 * @param ref      the booking reference (first 8 characters of its id)
 * @param booked   when it was booked
 * @param stamp    the stamp across it (DECLINED, CANCELLING, CANCELLED), or null for a valid ticket
 * @param stampCss the stamp's classes
 * @param bars     the decorative barcode
 * @author AoceanP
 */
public record BoardingPass(String code, String airline, String cabin, String price, String ref, String booked,
                           String stamp, String stampCss, List<Barcode.Bar> bars)
{
}
