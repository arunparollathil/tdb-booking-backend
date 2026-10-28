package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends DarshanBookingException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }

    public InvalidRequestException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }
}
