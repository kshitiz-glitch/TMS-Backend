package com.kshitizgaur.tms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a transporter tries to submit a duplicate bid for the
 * same load.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateBidException extends RuntimeException {

    public DuplicateBidException(String message) {
        super(message);
    }

    public DuplicateBidException() {
        super("Transporter has already submitted a bid for this load.");
    }
}
