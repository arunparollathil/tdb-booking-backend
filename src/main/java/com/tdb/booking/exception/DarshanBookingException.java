package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

public abstract class DarshanBookingException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected DarshanBookingException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
