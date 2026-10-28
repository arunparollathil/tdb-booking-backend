package com.tdb.booking.service;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import java.util.UUID;

public interface DarshanBookingService {

    DarshanBookingResponseDto createBooking(DarshanBookingRequestDto requestDto);

    DarshanBookingResponseDto getBookingById(UUID bookingId);
}
