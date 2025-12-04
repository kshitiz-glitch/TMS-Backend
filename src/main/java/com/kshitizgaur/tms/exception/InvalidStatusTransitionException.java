package com.kshitizgaur.tms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an invalid status transition is attempted.
 * For example, trying to cancel a load that is already BOOKED.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String entityName, String currentStatus, String attemptedAction) {
        super(String.format("Cannot %s %s with status '%s'", attemptedAction, entityName, currentStatus));
    }
}
