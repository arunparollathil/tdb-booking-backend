package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.BookingStatus;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.exception.IdempotencyConflictException;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.ResourceNotFoundException;
import com.tdb.booking.exception.SlotInactiveException;
import com.tdb.booking.mapper.DarshanBookingMapper;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.service.ReceiptNumberService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional executor for booking operations. Encapsulates pessimistic write locking, capacity
 * checks, idempotency verification, receipt number generation, and persistence within a single
 * atomic database transaction.
 */
@Component
@RequiredArgsConstructor
public class BookingTransactionExecutor {

    private static final Logger log = LoggerFactory.getLogger(BookingTransactionExecutor.class);

    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanBookingRepository darshanBookingRepository;
    private final DarshanBookingMapper darshanBookingMapper;
    private final ReceiptNumberService receiptNumberService;

    /**
     * Executes the ticket reservation workflow inside an atomic database transaction.
     *
     * <p>Concurrency & Idempotency sequence (P3 Safe Retries): 1. Pre-lock idempotency check:
     * returns existing booking if key already committed. 2. Pessimistic write lock: locks slot row
     * (SELECT ... FOR UPDATE). 3. Post-lock idempotency recheck: catches any concurrent duplicate
     * that committed while waiting for the slot lock. 4. Capacity validation & deduction while
     * holding slot write lock. 5. Receipt generation & DarshanBooking persistence with unique
     * idempotencyKey.
     *
     * @param requestDto booking request parameters
     * @return confirmed {@link DarshanBookingResponseDto}
     */
    @Transactional(rollbackFor = Exception.class)
    public DarshanBookingResponseDto executeBooking(DarshanBookingRequestDto requestDto) {
        UUID idempotencyKey = requestDto.getIdempotencyKey();

        // Step 1: Pre-lock idempotency check
        if (idempotencyKey != null) {
            Optional<DarshanBooking> existingOpt =
                    darshanBookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                DarshanBooking existing = existingOpt.get();
                validateMatchingBooking(existing, requestDto);
                log.info(
                        "Pre-lock idempotent retry detected for key: {}. Returning existing"
                                + " booking: {}",
                        idempotencyKey,
                        existing.getId());
                return darshanBookingMapper.toResponseDto(existing, "CONFIRMED");
            }
        }

        // Step 2: Retrieve and lock the darshan slot using pessimistic write lock (SELECT ... FOR
        // UPDATE)
        DarshanSlot slot =
                darshanSlotRepository
                        .findByIdForUpdate(requestDto.getDarshanSlotId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.slotNotFound(
                                                requestDto.getDarshanSlotId()));

        // Step 3: Post-lock idempotency recheck (Requirement 6)
        // Ensures that if a concurrent duplicate request committed while this thread was waiting
        // for
        // the slot lock, we return the existing booking rather than double-booking tickets.
        if (idempotencyKey != null) {
            Optional<DarshanBooking> existingOpt =
                    darshanBookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOpt.isPresent()) {
                DarshanBooking existing = existingOpt.get();
                validateMatchingBooking(existing, requestDto);
                log.info(
                        "Post-lock idempotent retry detected for key: {}. Returning existing"
                                + " booking: {}",
                        idempotencyKey,
                        existing.getId());
                return darshanBookingMapper.toResponseDto(existing, "CONFIRMED");
            }
        }

        // Step 4: Validate slot is active
        if (!Boolean.TRUE.equals(slot.getIsActive())) {
            throw SlotInactiveException.slotInactive(slot.getId());
        }

        // Step 5: Validate capacity while holding the write lock
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

        // Step 6: Deduct available tickets by incrementing booked_count
        slot.setBookedCount(slot.getBookedCount() + requestDto.getTicketCount());
        darshanSlotRepository.save(slot);

        // Step 7: Generate unique sequential receipt number within the same transaction
        String receiptNumber = receiptNumberService.generateReceiptNumber(slot.getTemple());

        // Step 8: Persist the DarshanBooking record with idempotencyKey, receiptNumber, and
        // CONFIRMED status
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(slot)
                        .idempotencyKey(idempotencyKey)
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
                "Booking confirmed successfully. BookingId: {}, Receipt: {}, Key: {}, Slot: {}"
                        + " (Booked: {}/{})",
                savedBooking.getId(),
                savedBooking.getReceiptNumber(),
                savedBooking.getIdempotencyKey(),
                slot.getId(),
                slot.getBookedCount(),
                slot.getCapacity());

        return darshanBookingMapper.toResponseDto(savedBooking, "CONFIRMED");
    }

    /**
     * Validates that a retried booking request matches the details of the original booking. Throws
     * {@link IdempotencyConflictException} (HTTP 409 Conflict) if any parameters differ.
     *
     * @param existing original persisted booking
     * @param request retried booking request payload
     */
    public static void validateMatchingBooking(
            DarshanBooking existing, DarshanBookingRequestDto request) {
        boolean slotMatches =
                existing.getDarshanSlot() != null
                        && existing.getDarshanSlot().getId().equals(request.getDarshanSlotId());
        boolean ticketsMatch =
                existing.getTicketCount() != null
                        && existing.getTicketCount().equals(request.getTicketCount());
        String existingDevotee =
                existing.getDevoteeName() != null ? existing.getDevoteeName().trim() : "";
        String requestDevotee =
                request.getDevoteeName() != null ? request.getDevoteeName().trim() : "";
        boolean devoteeMatches = existingDevotee.equalsIgnoreCase(requestDevotee);

        String existingPhone =
                existing.getPhoneNumber() != null ? existing.getPhoneNumber().trim() : "";
        String requestPhone =
                request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : "";
        boolean phoneMatches = existingPhone.equals(requestPhone);

        if (!slotMatches || !ticketsMatch || !devoteeMatches || !phoneMatches) {
            throw IdempotencyConflictException.payloadMismatch(
                    String.format(
                            "Idempotency key '%s' has already been used with different booking"
                                    + " details",
                            request.getIdempotencyKey()));
        }
    }
}
