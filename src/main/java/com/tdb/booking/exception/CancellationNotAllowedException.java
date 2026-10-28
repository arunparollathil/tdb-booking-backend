package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a booking cancellation request cannot be processed (for example, when the
 * slot has already started).
 */
public class CancellationNotAllowedException extends DarshanBookingException {

    public CancellationNotAllowedException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public static CancellationNotAllowedException slotAlreadyStarted(String message) {
        return new CancellationNotAllowedException("SLOT_ALREADY_STARTED", message);
    }
}
