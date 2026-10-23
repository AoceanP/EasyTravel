package com.easytravel.booking.dto;

import com.easytravel.booking.entity.Booking;
import com.easytravel.booking.entity.BookingStatus;
import com.easytravel.common.dto.FareClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * WHAT:   What clients see of a booking.
 * WHY:    The API returns this record, never the entity, so the table can
 *         change without breaking clients and internal fields such as the
 *         optimistic-lock version never leak.
 *
 * @param id            the booking id
 * @param userId        the customer
 * @param flightNumber  the flight
 * @param origin        departure airport
 * @param destination   arrival airport
 * @param fareClass     the cabin
 * @param price         the agreed price
 * @param status        PENDING, CONFIRMED or PAYMENT_FAILED
 * @param transactionId the payment's id, or null while pending
 * @param failureReason why payment failed, or null
 * @param createdAt     when it was created
 * @param updatedAt     when the status last changed
 * @author AoceanP
 */
public record BookingResponseDTO(
        UUID id,
        String userId,
        String flightNumber,
        String origin,
        String destination,
        FareClass fareClass,
        BigDecimal price,
        BookingStatus status,
        String transactionId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
)
{
    /**
     * WHAT:   Copies an entity into the public shape.
     * PRE:    booking is not null
     * RETURN: the DTO
     *
     * @param booking the entity
     * @return the DTO
     */
    public static BookingResponseDTO from(Booking booking)
    {
        return new BookingResponseDTO(booking.getId(), booking.getUserId(), booking.getFlightNumber(),
                booking.getOrigin(), booking.getDestination(), booking.getFareClass(), booking.getAgreedPrice(),
                booking.getStatus(), booking.getTransactionId(), booking.getFailureReason(),
                booking.getCreatedAt(), booking.getUpdatedAt());
    }
}
