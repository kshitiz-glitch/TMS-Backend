package com.kshitizgaur.tms.entity.enums;

/**
 * Represents the status of a Bid.
 */
public enum BidStatus {
    /** Bid is awaiting decision from shipper */
    PENDING,

    /** Bid has been accepted and booking created */
    ACCEPTED,

    /** Bid has been rejected by shipper */
    REJECTED
}
