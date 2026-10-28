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
     * Converts a {@link DarshanBooking} entity to a {@link DarshanBookingResponseDto}. Populates
     * the status from the entity's {@code bookingStatus} field.
     *
     * @param booking the persisted booking entity
     * @return populated {@link DarshanBookingResponseDto}, or null if input is null
     */
    public DarshanBookingResponseDto toResponseDto(DarshanBooking booking) {
        return toResponseDto(booking, null);
    }

    /**
     * Converts a {@link DarshanBooking} entity with an optional override for booking status.
     *
     * @param booking the persisted booking entity
     * @param bookingStatus confirmation status string override (e.g. "CONFIRMED", "CANCELLED")
     * @return populated {@link DarshanBookingResponseDto}, or null if input is null
     */
    public DarshanBookingResponseDto toResponseDto(DarshanBooking booking, String bookingStatus) {
        if (booking == null) {
            return null;
        }

        String resolvedStatus =
                bookingStatus != null
                        ? bookingStatus
                        : (booking.getBookingStatus() != null
                                ? booking.getBookingStatus().name()
                                : "CONFIRMED");

        return DarshanBookingResponseDto.builder()
                .bookingId(booking.getId())
                .idempotencyKey(booking.getIdempotencyKey())
                .darshanSlotId(
                        booking.getDarshanSlot() != null ? booking.getDarshanSlot().getId() : null)
                .receiptNumber(booking.getReceiptNumber())
                .devoteeName(booking.getDevoteeName())
                .phoneNumber(booking.getPhoneNumber())
                .ticketCount(booking.getTicketCount())
                .bookingStatus(resolvedStatus)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
