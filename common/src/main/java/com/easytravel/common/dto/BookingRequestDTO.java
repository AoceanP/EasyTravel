package com.easytravel.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author aleksandar panich
 * @version 1.0
 *
 * Immutable contract for an incoming "create booking" request.
 *
 * <p>This is an INPUT DTO: it arrives from an untrusted client, so every field
 * carries a Bean Validation constraint. The constraints live ON the contract,
 * so any service that accepts this shape enforces the same rules automatically.
 *
 * @param userId       the customer creating the booking
 * @param flightNumber the flight being booked (matches FlightDTO#flightNumber)
 * @param amount       the fare to be charged; validated as strictly positive
 */
public record BookingRequestDTO(

        @NotBlank(message = "userId must not be blank")
        String userId,

        @NotBlank(message = "flightNumber must not be blank")
        String flightNumber,

        @NotNull(message = "amount is required")
        BigDecimal amount
)
{}