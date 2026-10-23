package com.easytravel.web.gateway;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * WHAT:   A booking as booking-service returns it through the gateway.
 * WHY:    The Book page shows the server's own price and status, never values
 *         it computed itself.
 *
 * @param id            the booking's UUID
 * @param userId        who booked
 * @param flightNumber  the flight
 * @param origin        IATA code
 * @param destination   IATA code
 * @param fareClass     ECONOMY, BUSINESS or FIRST
 * @param price         the price the server charged
 * @param status        PENDING, CONFIRMED, PAYMENT_FAILED, CANCELLING or CANCELLED
 * @param transactionId the payment's id, once there is one
 * @param failureReason why payment (or a refund) failed, or null
 * @param createdAt     when it was created
 * @param updatedAt     when its status last changed
 * @author AoceanP
 */
public record BookingDto(String id, String userId, String flightNumber, String origin, String destination,
                         String fareClass, BigDecimal price, String status, String transactionId,
                         String failureReason, Instant createdAt, Instant updatedAt)
{
    /**
     * WHAT:   Whether payment has decided this booking yet.
     * PRE:    none
     * RETURN: true for CONFIRMED or PAYMENT_FAILED
     *
     * @return whether the booking is decided
     */
    public boolean isDecided()
    {
        return "CONFIRMED".equals(status) || "PAYMENT_FAILED".equals(status);
    }
}
