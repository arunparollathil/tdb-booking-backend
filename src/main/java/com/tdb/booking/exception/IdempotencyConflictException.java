package com.tdb.booking.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a client reuses an idempotency key with conflicting or altered booking
 * details. Maps to HTTP 409 Conflict with error code IDEMPOTENCY_CONFLICT.
 */
public class IdempotencyConflictException extends DarshanBookingException {

    public IdempotencyConflictException(String message) {
        super(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", message);
    }

    public static IdempotencyConflictException payloadMismatch(String message) {
        return new IdempotencyConflictException(message);
    }
}
