package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.InvalidRequestException;
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
 * database row locking (LockModeType.PESSIMISTIC_WRITE) to guarantee that capacity limits are
 * strictly enforced, even under heavy concurrent requests from multiple instances of the service.
 */
@Service
@RequiredArgsConstructor
public class DarshanBookingServiceImpl implements DarshanBookingService {

    private static final Logger log = LoggerFactory.getLogger(DarshanBookingServiceImpl.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;

    /**
     * Confirms ticket reservation for a darshan slot using pessimistic database row locking.
     *
     * <p>Concurrency Strategy (P2 - No Overbooking): 1. Validate the request and ensure ticketCount
     * > 0. 2. Begin a database transaction (@Transactional). 3. Retrieve and lock the darshan slot
     * using findByIdForUpdate() (SELECT ... FOR UPDATE). 4. Return standard 404 error if the slot
     * does not exist. 5. Reject inactive slots with 409 Conflict using existing error-handling
     * conventions. 6. Calculate available ticket count while holding the exclusive lock. 7. If the
     * requested tickets exceed availability, throw InsufficientCapacityException. 8. Increment
     * booked_count by the requested ticket count. 9. Create and persist the DarshanBooking record
     * with audit fields. 10. Commit the transaction after successful completion. If an error
     * occurs, both the booked-count update and the booking insertion roll back atomically.
     *
     * @param requestDto booking request details
     * @return confirmed {@link DarshanBookingResponseDto}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DarshanBookingResponseDto createBooking(DarshanBookingRequestDto requestDto) {
        // Step 1: Validate the request and ensure ticketCount > 0
        if (requestDto == null) {
            throw new InvalidRequestException("Booking request payload cannot be null");
        }
        if (requestDto.getDarshanSlotId() == null) {
            throw new InvalidRequestException("Darshan slot ID is mandatory");
        }
        if (requestDto.getTicketCount() == null || requestDto.getTicketCount() <= 0) {
            throw new InvalidRequestException("Ticket count must be greater than zero");
        }

        log.info(
                "Attempting booking for slotId: {}, devotee: {}, tickets: {}",
                requestDto.getDarshanSlotId(),
                requestDto.getDevoteeName(),
                requestDto.getTicketCount());

        // Step 2 & 3: Retrieve and lock the darshan slot using findByIdForUpdate() (SELECT ... FOR
        // UPDATE)
        DarshanSlot slot =
                darshanSlotRepository
                        .findByIdForUpdate(requestDto.getDarshanSlotId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.slotNotFound(
                                                requestDto.getDarshanSlotId()));

        // Step 4 & 5: Reject inactive slots using existing error-handling conventions
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw SlotInactiveException.slotInactive(slot.getId());
        }

        // Step 6: Calculate the available ticket count while holding the lock
        int availableTickets = slot.getAvailableCount();

        // Step 7: If the requested tickets exceed availability, throw an appropriate business
        // exception
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

        // Step 8: Increment booked_count by the requested ticket count (JPA managed entity dirty
        // checking)
        slot.setBookedCount(slot.getBookedCount() + requestDto.getTicketCount());
        darshanSlotRepository.save(slot);

        // Step 9 & 10: Create and persist the DarshanBooking record with audit fields
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(slot)
                        .devoteeName(requestDto.getDevoteeName().trim())
                        .phoneNumber(requestDto.getPhoneNumber().trim())
                        .ticketCount(requestDto.getTicketCount())
                        .createdBy(UUID.randomUUID())
                        .build();

        DarshanBooking savedBooking = darshanBookingRepository.save(booking);

        log.info(
                "Booking successful. BookingId: {}, New BookedCount: {}/{}",
                savedBooking.getId(),
                slot.getBookedCount(),
                slot.getCapacity());

        // Step 11 & 12: Return the existing booking response DTO and commit transaction
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
