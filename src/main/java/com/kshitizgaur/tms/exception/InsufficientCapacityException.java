package com.kshitizgaur.tms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a transporter doesn't have sufficient truck capacity.
 * This enforces Rule 1: Capacity Validation.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientCapacityException extends RuntimeException {

    public InsufficientCapacityException(String message) {
        super(message);
    }

    public InsufficientCapacityException(String truckType, int requested, int available) {
        super(String.format("Insufficient capacity for truck type '%s'. Requested: %d, Available: %d",
                truckType, requested, available));
    }
}
