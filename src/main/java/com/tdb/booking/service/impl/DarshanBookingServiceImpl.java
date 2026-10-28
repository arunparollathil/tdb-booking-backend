package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.BookingStatus;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.exception.CancellationNotAllowedException;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.exception.ResourceNotFoundException;
import com.tdb.booking.mapper.DarshanBookingMapper;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.service.DarshanBookingService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link DarshanBookingService}. Executes ticket reservations and cancellations
 * using pessimistic database row locking (LockModeType.PESSIMISTIC_WRITE) and safe idempotency key
 * retries.
 */
@Service
@RequiredArgsConstructor
public class DarshanBookingServiceImpl implements DarshanBookingService {

    private static final Logger log = LoggerFactory.getLogger(DarshanBookingServiceImpl.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;
    private final BookingTransactionExecutor bookingTransactionExecutor;

    /**
     * Confirms ticket reservation for a darshan slot using pessimistic database row locking and
     * safe idempotency retries.
     *
     * @param requestDto booking request details
     * @return confirmed {@link DarshanBookingResponseDto}
     */
    @Override
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

        // Auto-assign idempotency key if not supplied by client (ensuring backward compatibility)
        if (requestDto.getIdempotencyKey() == null) {
            requestDto.setIdempotencyKey(UUID.randomUUID());
        }

        log.info(
                "Attempting booking for slotId: {}, key: {}, devotee: {}, tickets: {}",
                requestDto.getDarshanSlotId(),
                requestDto.getIdempotencyKey(),
                requestDto.getDevoteeName(),
                requestDto.getTicketCount());

        try {
            return bookingTransactionExecutor.executeBooking(requestDto);
        } catch (DataIntegrityViolationException ex) {
            log.warn(
                    "Database constraint collision on idempotency key: {}. Attempting safe"
                            + " recovery.",
                    requestDto.getIdempotencyKey(),
                    ex);
            return recoverCommittedBooking(requestDto, ex);
        }
    }

    /**
     * Recovers the already-committed booking when a concurrent unique constraint race condition
     * occurs. Validates that payload parameters match, and returns the committed booking.
     *
     * @param requestDto current booking request
     * @param originalEx original database constraint violation exception
     * @return existing {@link DarshanBookingResponseDto}
     */
    private DarshanBookingResponseDto recoverCommittedBooking(
            DarshanBookingRequestDto requestDto, DataIntegrityViolationException originalEx) {
        Optional<DarshanBooking> existingOpt =
                darshanBookingRepository.findByIdempotencyKey(requestDto.getIdempotencyKey());

        if (existingOpt.isPresent()) {
            DarshanBooking existing = existingOpt.get();
            BookingTransactionExecutor.validateMatchingBooking(existing, requestDto);
            log.info(
                    "Successfully recovered committed booking {} for idempotency key {}",
                    existing.getId(),
                    requestDto.getIdempotencyKey());
            return darshanBookingMapper.toResponseDto(existing, "CONFIRMED");
        }

        throw originalEx;
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
