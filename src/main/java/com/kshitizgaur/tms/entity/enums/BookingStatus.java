package com.kshitizgaur.tms.entity.enums;

/**
 * Represents the status of a Booking.
 */
public enum BookingStatus {
    /** Booking is confirmed and trucks are allocated */
    CONFIRMED,

    /** Booking has been completed successfully */
    COMPLETED,

    /** Booking has been cancelled, trucks restored to pool */
    CANCELLED
}
