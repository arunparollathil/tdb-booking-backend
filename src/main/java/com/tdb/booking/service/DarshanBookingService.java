package com.tdb.booking.service;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import java.util.UUID;

/** Service interface for managing darshan ticket reservations and cancellations. */
public interface DarshanBookingService {

    /**
     * Books one or more tickets on a darshan slot under a transactional pessimistic lock.
     *
     * @param requestDto booking request containing slot ID, devotee info, and ticket count
     * @return {@link DarshanBookingResponseDto} with confirmed booking details
     */
    DarshanBookingResponseDto createBooking(DarshanBookingRequestDto requestDto);

    /**
     * Retrieves an existing booking reservation by its unique reference UUID.
     *
     * @param bookingId unique booking reference
     * @return {@link DarshanBookingResponseDto}
     */
    DarshanBookingResponseDto getBookingById(UUID bookingId);

    /**
     * Cancels an existing confirmed booking and releases reserved tickets back to the slot.
     * Executed under a pessimistic write lock on the slot. Idempotent if already cancelled.
     *
     * @param bookingId unique identifier of the booking to cancel
     * @return updated {@link DarshanBookingResponseDto} with CANCELLED status
     */
    DarshanBookingResponseDto cancelBooking(UUID bookingId);
}
