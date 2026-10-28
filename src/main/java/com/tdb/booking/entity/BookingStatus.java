package com.tdb.booking.entity;

/** Enumeration representing the lifecycle states of a darshan ticket booking. */
public enum BookingStatus {

    /** Booking is confirmed and tickets are reserved on the darshan slot. */
    CONFIRMED,

    /** Booking is cancelled and reserved tickets have been released back to the darshan slot. */
    CANCELLED
}
