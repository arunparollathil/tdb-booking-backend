package com.tdb.booking.mapper;

import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.DarshanBooking;
import org.springframework.stereotype.Component;

@Component
public class DarshanBookingMapper {

    public DarshanBookingResponseDto toResponseDto(DarshanBooking booking) {
        return toResponseDto(booking, null);
    }

    public DarshanBookingResponseDto toResponseDto(DarshanBooking booking, String bookingStatus) {
        if (booking == null) {
            return null;
        }

        return DarshanBookingResponseDto.builder()
                .bookingId(booking.getId())
                .darshanSlotId(
                        booking.getDarshanSlot() != null ? booking.getDarshanSlot().getId() : null)
                .devoteeName(booking.getDevoteeName())
                .phoneNumber(booking.getPhoneNumber())
                .ticketCount(booking.getTicketCount())
                .bookingStatus(bookingStatus)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
