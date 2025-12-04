package com.kshitizgaur.tms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a concurrent booking conflict occurs.
 * This is triggered by OptimisticLockException during concurrent truck
 * allocation.
 * Enforces Rule 4: Concurrent Booking Prevention.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class LoadAlreadyBookedException extends RuntimeException {

    public LoadAlreadyBookedException(String message) {
        super(message);
    }

    public LoadAlreadyBookedException() {
        super("Booking conflict detected. Another transaction modified the resource. Please retry.");
    }
}
