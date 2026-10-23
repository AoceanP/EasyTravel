package com.easytravel.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * WHAT:   The body of a "create booking" request.
 * WHY:    It deliberately has NO price field. An earlier version let the
 *         client send the amount, so anyone could book a flight for $1;
 *         now the booking service asks the inventory service for the real
 *         fare. Never trust a price from the client.
 *         Every field is validated here, on the contract, so any service
 *         accepting this shape enforces the same rules.
 * PRE:    arrives from an untrusted client; @Valid on the controller runs the checks
 *
 * @param userId       the customer making the booking
 * @param flightNumber the flight to book, e.g. "BA249" (from a flight search)
 * @param origin       the 3-letter IATA departure airport, e.g. "LHR"
 * @param destination  the 3-letter IATA arrival airport, e.g. "JFK"
 * @param fareClass    the cabin; changes the price
 * @author AoceanP
 */
public record BookingRequestDTO(

        @NotBlank(message = "userId must not be blank")
        String userId,

        @NotBlank(message = "flightNumber must not be blank")
        String flightNumber,

        @Pattern(regexp = "[A-Z]{3}", message = "origin must be a 3-letter airport code, e.g. LHR")
        @NotNull(message = "origin is required")
        String origin,

        @Pattern(regexp = "[A-Z]{3}", message = "destination must be a 3-letter airport code, e.g. JFK")
        @NotNull(message = "destination is required")
        String destination,

        @NotNull(message = "fareClass is required: ECONOMY, BUSINESS or FIRST")
        FareClass fareClass
)
{
}
