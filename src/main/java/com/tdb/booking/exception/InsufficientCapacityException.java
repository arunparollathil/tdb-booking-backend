package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

public class InsufficientCapacityException extends DarshanBookingException {

    public InsufficientCapacityException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_CAPACITY", message);
    }
}
