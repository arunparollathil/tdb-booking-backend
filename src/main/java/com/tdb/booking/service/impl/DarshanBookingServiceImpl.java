package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.ResourceNotFoundException;
import com.tdb.booking.exception.SlotInactiveException;
import com.tdb.booking.mapper.DarshanBookingMapper;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.service.DarshanBookingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DarshanBookingServiceImpl implements DarshanBookingService {

    private static final Logger log = LoggerFactory.getLogger(DarshanBookingServiceImpl.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;

    @Override
    @Transactional
    public DarshanBookingResponseDto createBooking(DarshanBookingRequestDto requestDto) {
        log.info(
                "Attempting booking for slotId: {}, devotee: {}, tickets: {}",
                requestDto.getDarshanSlotId(),
                requestDto.getDevoteeName(),
                requestDto.getTicketCount());

        // 1. Retrieve the darshan slot with a pessimistic lock to ensure thread safety
        DarshanSlot slot =
                darshanSlotRepository
                        .findByIdWithLock(requestDto.getDarshanSlotId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.slotNotFound(
                                                requestDto.getDarshanSlotId()));

        // 2. Verify the slot is active
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw SlotInactiveException.slotInactive(slot.getId());
        }

        // 3. Check capacity availability
        int availableTickets = slot.getAvailableCount();
        if (requestDto.getTicketCount() > availableTickets) {
            log.warn(
                    "Insufficient capacity for slotId: {}. Requested: {}, Available: {}",
                    slot.getId(),
                    requestDto.getTicketCount(),
                    availableTickets);
            throw new InsufficientCapacityException(
                    String.format(
                            "Requested tickets (%d) exceed available capacity (%d)",
                            requestDto.getTicketCount(), availableTickets));
        }

        // 4. Create and persist booking record
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(slot)
                        .devoteeName(requestDto.getDevoteeName().trim())
                        .phoneNumber(requestDto.getPhoneNumber().trim())
                        .ticketCount(requestDto.getTicketCount())
                        .createdBy(UUID.randomUUID())
                        .build();

        DarshanBooking savedBooking = darshanBookingRepository.save(booking);

        // 5. Increment booked count and persist slot update
        slot.setBookedCount(slot.getBookedCount() + requestDto.getTicketCount());
        darshanSlotRepository.save(slot);

        log.info(
                "Booking successful. BookingId: {}, New BookedCount: {}",
                savedBooking.getId(),
                slot.getBookedCount());

        return darshanBookingMapper.toResponseDto(savedBooking, "CONFIRMED");
    }

    @Override
    @Transactional(readOnly = true)
    public DarshanBookingResponseDto getBookingById(UUID bookingId) {
        log.info("Fetching darshan booking with id: {}", bookingId);

        DarshanBooking booking =
                darshanBookingRepository
                        .findById(bookingId)
                        .orElseThrow(() -> ResourceNotFoundException.bookingNotFound(bookingId));

        return darshanBookingMapper.toResponseDto(booking);
    }
}
