package com.easytravel.booking.entity;

/**
 * Cabin / fare class the customer booked. Kept as an enum so downstream logic
 * (pricing rules, and the seat-inventory limits we build next step) can switch on it
 * exhaustively and safely.
 */
public enum FareClass {
    ECONOMY,
    BUSINESS,
    FIRST
}