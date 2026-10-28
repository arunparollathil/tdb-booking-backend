package com.tdb.booking.mapper;

import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.DarshanBooking;
import org.springframework.stereotype.Component;

/**
 * Mapper component for transforming {@link DarshanBooking} entities to {@link
 * DarshanBookingResponseDto}.
 */
@Component
public class DarshanBookingMapper {

    /**
     * Converts a {@link DarshanBooking} entity to a {@link DarshanBookingResponseDto}.
     *
     * @param booking the persisted booking entity
     * @return populated {@link DarshanBookingResponseDto}, or null if input is null
     */
    public DarshanBookingResponseDto toResponseDto(DarshanBooking booking) {
        return toResponseDto(booking, null);
    }

    /**
     * Converts a {@link DarshanBooking} entity with an optional booking status.
     *
     * @param booking the persisted booking entity
     * @param bookingStatus confirmation status string (e.g. "CONFIRMED")
     * @return populated {@link DarshanBookingResponseDto}, or null if input is null
     */
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
