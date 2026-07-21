package com.easytravel.booking.entity;

/**
 * Lifecycle states of a booking. A booking is born PENDING and becomes CONFIRMED once
 * payment succeeds (Phase 3.2, via a RabbitMQ event). An enum — not a free String —
 * makes the valid states a compile-time contract; you can't persist a typo'd "PENDNIG".
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}