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

/**
 * Implementation of {@link DarshanBookingService}. Executes ticket reservations using pessimistic
 * database locking to guarantee atomic capacity verification and concurrency safety.
 */
@Service
@RequiredArgsConstructor
public class DarshanBookingServiceImpl implements DarshanBookingService {

    private static final Logger log = LoggerFactory.getLogger(DarshanBookingServiceImpl.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;

    /**
     * Confirms ticket reservation for a darshan slot.
     *
     * <p>Concurrency Strategy: Reads the slot row with {@code PESSIMISTIC_WRITE} database locking
     * (SELECT ... FOR UPDATE). This prevents simultaneous transactions from concurrently reading
     * stale capacity and overbooking. Both the booking creation and the slot booked_count update
     * are committed atomically.
     *
     * @param requestDto booking request details
     * @return confirmed {@link DarshanBookingResponseDto}
     */
    @Override
    @Transactional
    public DarshanBookingResponseDto createBooking(DarshanBookingRequestDto requestDto) {
        log.info(
                "Attempting booking for slotId: {}, devotee: {}, tickets: {}",
                requestDto.getDarshanSlotId(),
                requestDto.getDevoteeName(),
                requestDto.getTicketCount());

        // Step 1: Retrieve the darshan slot with a pessimistic write lock (SELECT FOR UPDATE)
        DarshanSlot slot =
                darshanSlotRepository
                        .findByIdWithLock(requestDto.getDarshanSlotId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.slotNotFound(
                                                requestDto.getDarshanSlotId()));

        // Step 2: Verify that the slot exists and is active
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw SlotInactiveException.slotInactive(slot.getId());
        }

        // Step 3: Check that available tickets are sufficient
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

        // Step 4: Create the booking record
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(slot)
                        .devoteeName(requestDto.getDevoteeName().trim())
                        .phoneNumber(requestDto.getPhoneNumber().trim())
                        .ticketCount(requestDto.getTicketCount())
                        .createdBy(UUID.randomUUID())
                        .build();

        DarshanBooking savedBooking = darshanBookingRepository.save(booking);

        // Step 5: Increment darshan_slot.booked_count by the requested ticket count
        slot.setBookedCount(slot.getBookedCount() + requestDto.getTicketCount());
        darshanSlotRepository.save(slot);

        log.info(
                "Booking successful. BookingId: {}, New BookedCount: {}",
                savedBooking.getId(),
                slot.getBookedCount());

        // Step 6: Return the confirmed booking response
        return darshanBookingMapper.toResponseDto(savedBooking, "CONFIRMED");
    }

    /**
     * Retrieves an existing booking reservation by its UUID reference.
     *
     * @param bookingId unique booking reference
     * @return {@link DarshanBookingResponseDto}
     */
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
