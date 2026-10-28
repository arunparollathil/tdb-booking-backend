package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.BookingStatus;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.exception.CancellationNotAllowedException;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.exception.ResourceNotFoundException;
import com.tdb.booking.exception.SlotInactiveException;
import com.tdb.booking.mapper.DarshanBookingMapper;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.service.DarshanBookingService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link DarshanBookingService}. Executes ticket reservations and cancellations
 * using pessimistic database row locking (LockModeType.PESSIMISTIC_WRITE) to guarantee that
 * capacity limits and slot schedules are strictly enforced, even under heavy concurrent requests.
 */
@Service
@RequiredArgsConstructor
public class DarshanBookingServiceImpl implements DarshanBookingService {

    private static final Logger log = LoggerFactory.getLogger(DarshanBookingServiceImpl.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;
    private final com.tdb.booking.service.ReceiptNumberService receiptNumberService;

    /**
     * Confirms ticket reservation for a darshan slot using pessimistic database row locking.
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

        // Step 6: Calculate available ticket count while holding the lock
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

        // Step 8: Increment booked_count by the requested ticket count (JPA dirty checking /
        // managed entity)
        slot.setBookedCount(slot.getBookedCount() + requestDto.getTicketCount());
        darshanSlotRepository.save(slot);

        // Step 9: Generate unique, concurrent-safe receipt number in format
        // TEMPLE_CODE/FINANCIAL_YEAR/SEQUENCE
        String receiptNumber = receiptNumberService.generateReceiptNumber(slot.getTemple());

        // Step 10: Create and persist the DarshanBooking record with audit fields, CONFIRMED
        // status,
        // and receipt number
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(slot)
                        .receiptNumber(receiptNumber)
                        .devoteeName(
                                requestDto.getDevoteeName() != null
                                        ? requestDto.getDevoteeName().trim()
                                        : "")
                        .phoneNumber(
                                requestDto.getPhoneNumber() != null
                                        ? requestDto.getPhoneNumber().trim()
                                        : "")
                        .ticketCount(requestDto.getTicketCount())
                        .bookingStatus(BookingStatus.CONFIRMED)
                        .createdBy(UUID.randomUUID())
                        .build();

        DarshanBooking savedBooking = darshanBookingRepository.save(booking);

        log.info(
                "Booking successful. BookingId: {}, Receipt: {}, New BookedCount: {}/{}",
                savedBooking.getId(),
                savedBooking.getReceiptNumber(),
                slot.getBookedCount(),
                slot.getCapacity());

        // Step 11 & 12: Return the confirmed booking response DTO and commit transaction
        return darshanBookingMapper.toResponseDto(savedBooking, "CONFIRMED");
    }

    /**
     * Cancels an existing booking and releases its tickets back to the slot under a pessimistic
     * write lock.
     *
     * <p>Implementation Sequence (P4 - Cancellation): 1. Fetch the booking. Return 404 if not
     * found. 2. Acquire a pessimistic write lock on the associated slot (findByIdForUpdate). 3.
     * Validate the booking: - If already cancelled, return the existing cancelled booking without
     * changing booked_count (idempotent). - If the slot has already started, reject cancellation
     * with 409 Conflict. 4. Update the booking status to CANCELLED. 5. Decrease booked_count by the
     * booking's ticket_count. 6. Commit both changes in the same transaction.
     *
     * @param bookingId unique identifier of the booking to cancel
     * @return {@link DarshanBookingResponseDto} with updated CANCELLED status
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DarshanBookingResponseDto cancelBooking(UUID bookingId) {
        if (bookingId == null) {
            throw new InvalidRequestException("Booking ID is mandatory");
        }

        log.info("Processing cancellation for bookingId: {}", bookingId);

        // Step 1: Fetch the booking (throws 404 if not found)
        DarshanBooking booking =
                darshanBookingRepository
                        .findById(bookingId)
                        .orElseThrow(() -> ResourceNotFoundException.bookingNotFound(bookingId));

        // Step 2: Acquire a pessimistic write lock on the slot before checking state and capacity
        UUID slotId = booking.getDarshanSlot().getId();
        DarshanSlot slot =
                darshanSlotRepository
                        .findByIdForUpdate(slotId)
                        .orElseThrow(() -> ResourceNotFoundException.slotNotFound(slotId));

        // Step 3a: Idempotency - if already cancelled, return existing cancelled booking without
        // releasing tickets again
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            log.info(
                    "Booking {} is already cancelled. Idempotently returning existing cancelled"
                            + " booking.",
                    bookingId);
            return darshanBookingMapper.toResponseDto(booking);
        }

        // Step 3b: If the slot has already started, reject cancellation with 409 Conflict
        LocalDateTime slotStartDateTime =
                LocalDateTime.of(slot.getSlotDate(), slot.getSlotMaster().getStartTime());
        if (LocalDateTime.now().isAfter(slotStartDateTime)) {
            log.warn(
                    "Cancellation rejected for booking {}: Darshan slot already started at {}",
                    bookingId,
                    slotStartDateTime);
            throw CancellationNotAllowedException.slotAlreadyStarted(
                    "Cannot cancel booking because the darshan slot has already started at "
                            + slotStartDateTime);
        }

        // Step 4: Update the booking status to CANCELLED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        DarshanBooking updatedBooking = darshanBookingRepository.save(booking);

        // Step 5: Decrease booked_count by the booking's ticket_count
        int updatedBookedCount = Math.max(0, slot.getBookedCount() - booking.getTicketCount());
        slot.setBookedCount(updatedBookedCount);
        darshanSlotRepository.save(slot);

        log.info(
                "Booking {} cancelled successfully. Slot {} bookedCount decreased to {}/{}",
                bookingId,
                slot.getId(),
                slot.getBookedCount(),
                slot.getCapacity());

        // Step 6: Return the updated booking response
        return darshanBookingMapper.toResponseDto(updatedBooking);
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
