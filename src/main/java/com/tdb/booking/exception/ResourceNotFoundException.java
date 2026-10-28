package com.tdb.booking.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends DarshanBookingException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }

    public static ResourceNotFoundException slotNotFound(UUID slotId) {
        return new ResourceNotFoundException(
                "SLOT_NOT_FOUND", "Darshan slot not found with id: " + slotId);
    }

    public static ResourceNotFoundException bookingNotFound(UUID bookingId) {
        return new ResourceNotFoundException(
                "BOOKING_NOT_FOUND", "Darshan booking not found with id: " + bookingId);
    }

    public static ResourceNotFoundException templeNotFound(UUID templeId) {
        return new ResourceNotFoundException(
                "TEMPLE_NOT_FOUND", "Temple not found with id: " + templeId);
    }

    public static ResourceNotFoundException slotMasterNotFound(UUID slotMasterId) {
        return new ResourceNotFoundException(
                "SLOT_MASTER_NOT_FOUND", "Slot master not found with id: " + slotMasterId);
    }
}
