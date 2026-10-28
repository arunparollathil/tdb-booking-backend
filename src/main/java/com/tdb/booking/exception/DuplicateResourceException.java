package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends DarshanBookingException {

    public DuplicateResourceException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public static DuplicateResourceException duplicateSlot(String message) {
        return new DuplicateResourceException("DUPLICATE_SLOT", message);
    }
}
