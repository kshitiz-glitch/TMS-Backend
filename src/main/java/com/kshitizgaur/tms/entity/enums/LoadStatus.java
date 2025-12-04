package com.kshitizgaur.tms.entity.enums;

/**
 * Represents the lifecycle status of a Load.
 * 
 * State Transitions:
 * - POSTED → OPEN_FOR_BIDS (when first bid received)
 * - OPEN_FOR_BIDS → BOOKED (when all trucks allocated)
 * - POSTED/OPEN_FOR_BIDS → CANCELLED (when shipper cancels)
 */
public enum LoadStatus {
    /** Load is newly created and visible for transporters */
    POSTED,

    /** Load has received at least one bid and is open for more */
    OPEN_FOR_BIDS,

    /** All trucks for the load have been allocated through bookings */
    BOOKED,

    /** Load has been cancelled by the shipper */
    CANCELLED
}
