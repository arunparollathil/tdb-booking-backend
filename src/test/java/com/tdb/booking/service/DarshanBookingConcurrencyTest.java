package com.tdb.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.dto.DarshanBookingResponseDto;
import com.tdb.booking.entity.BookingStatus;
import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.entity.TempleReceiptSequence;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.repository.SlotMasterRepository;
import com.tdb.booking.repository.TempleMasterRepository;
import com.tdb.booking.repository.TempleReceiptSequenceRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency tests verifying P2 – No Overbooking requirement. Proves that pessimistic row-level
 * locking (LockModeType.PESSIMISTIC_WRITE) guarantees capacity limits are strictly respected under
 * simultaneous multi-threaded booking attempts.
 */
@SpringBootTest
class DarshanBookingConcurrencyTest {

    @Autowired private DarshanBookingService darshanBookingService;

    @Autowired private ReceiptNumberService receiptNumberService;

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    @Autowired private DarshanBookingRepository darshanBookingRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private TempleMasterRepository templeMasterRepository;

    @Autowired private TempleReceiptSequenceRepository templeReceiptSequenceRepository;

    private TempleMaster temple;
    private DarshanSlot darshanSlot;

    @BeforeEach
    void setUp() {
        darshanBookingRepository.deleteAll();
        darshanSlotRepository.deleteAll();
        slotMasterRepository.deleteAll();
        templeReceiptSequenceRepository.deleteAll();
        templeMasterRepository.deleteAll();

        temple =
                templeMasterRepository.save(
                        TempleMaster.builder()
                                .templeCode(
                                        "CONCUR_" + UUID.randomUUID().toString().substring(0, 8))
                                .templeName("Concurrency Test Temple")
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        SlotMaster slotMaster =
                slotMasterRepository.save(
                        SlotMaster.builder()
                                .temple(temple)
                                .slotName("Concurrent Slot")
                                .startTime(LocalTime.of(8, 0))
                                .endTime(LocalTime.of(9, 0))
                                .defaultCapacity(10)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        darshanSlot =
                darshanSlotRepository.save(
                        DarshanSlot.builder()
                                .temple(temple)
                                .slotMaster(slotMaster)
                                .slotDate(LocalDate.now().plusDays(10))
                                .capacity(10)
                                .bookedCount(0)
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());
    }

    @Test
    @DisplayName(
            "P2: 10 concurrent requests of 2 tickets on capacity 10 -> exactly 5 succeed, 5"
                    + " rejected, no overbooking")
    void concurrentBookings_ShouldNotExceedCapacity() throws InterruptedException {
        int numberOfThreads = 10;
        int ticketsPerRequest = 2; // Total requested = 20, Capacity = 10

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            darshanBookingService.createBooking(
                                    DarshanBookingRequestDto.builder()
                                            .darshanSlotId(darshanSlot.getId())
                                            .devoteeName("Devotee " + index)
                                            .phoneNumber("987654321" + (index % 10))
                                            .ticketCount(ticketsPerRequest)
                                            .build());
                            successCount.incrementAndGet();
                        } catch (InsufficientCapacityException e) {
                            failureCount.incrementAndGet();
                        } catch (Exception e) {
                            // unexpected
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        startLatch.countDown(); // start all threads simultaneously
        endLatch.await();
        executorService.shutdown();

        // Exactly 5 requests of 2 tickets each should succeed (10 tickets total)
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(5);

        // Verify slot state in database: bookedCount == 10 and availableCount == 0
        DarshanSlot updatedSlot = darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        assertThat(updatedSlot.getBookedCount()).isEqualTo(10);
        assertThat(updatedSlot.getAvailableCount()).isEqualTo(0);
        assertThat(darshanBookingRepository.findByDarshanSlotId(darshanSlot.getId())).hasSize(5);
    }

    @Test
    @DisplayName(
            "P2: 25 concurrent threads each booking 1 ticket on capacity 10 -> exactly 10 succeed,"
                    + " 15 rejected")
    void concurrentSingleTicketBookings_ShouldSellExactlyCapacity() throws InterruptedException {
        int numberOfThreads = 25;
        int ticketsPerRequest = 1; // Total requested = 25, Capacity = 10

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            darshanBookingService.createBooking(
                                    DarshanBookingRequestDto.builder()
                                            .darshanSlotId(darshanSlot.getId())
                                            .devoteeName("Devotee " + index)
                                            .phoneNumber(
                                                    "98765432" + (index < 10 ? "0" + index : index))
                                            .ticketCount(ticketsPerRequest)
                                            .build());
                            successCount.incrementAndGet();
                        } catch (InsufficientCapacityException e) {
                            failureCount.incrementAndGet();
                        } catch (Exception e) {
                            // unexpected
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(15);

        DarshanSlot updatedSlot = darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        assertThat(updatedSlot.getBookedCount()).isEqualTo(10);
        assertThat(updatedSlot.getAvailableCount()).isEqualTo(0);
        assertThat(darshanBookingRepository.findByDarshanSlotId(darshanSlot.getId())).hasSize(10);
    }

    @Test
    @DisplayName(
            "P2: findByIdForUpdate query executes successfully with pessimistic row lock inside a"
                    + " transaction")
    @Transactional
    void findByIdForUpdate_ShouldReturnLockedSlot() {
        Optional<DarshanSlot> lockedSlot =
                darshanSlotRepository.findByIdForUpdate(darshanSlot.getId());
        assertThat(lockedSlot).isPresent();
        assertThat(lockedSlot.get().getId()).isEqualTo(darshanSlot.getId());
        assertThat(lockedSlot.get().getCapacity()).isEqualTo(10);
    }

    @Test
    @DisplayName(
            "P2: Service validation rejects non-positive ticket count and does not alter capacity")
    void invalidTicketCount_ShouldThrowExceptionAndNotAlterCapacity() {
        DarshanBookingRequestDto request =
                DarshanBookingRequestDto.builder()
                        .darshanSlotId(darshanSlot.getId())
                        .devoteeName("Devotee Zero")
                        .phoneNumber("9876543210")
                        .ticketCount(0)
                        .build();

        assertThatThrownBy(() -> darshanBookingService.createBooking(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Ticket count must be greater than zero");

        DarshanSlot unalteredSlot =
                darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        assertThat(unalteredSlot.getBookedCount()).isEqualTo(0);
        assertThat(unalteredSlot.getAvailableCount()).isEqualTo(10);
    }

    @Test
    @DisplayName(
            "P4: Concurrent cancellation of multiple bookings correctly releases all reserved"
                    + " tickets")
    void concurrentCancellations_ShouldCorrectlyReleaseAllTickets() throws InterruptedException {
        int numberOfBookings = 5;
        java.util.List<UUID> bookingIds = new java.util.ArrayList<>();

        // Create 5 bookings of 2 tickets each (total 10 tickets booked, filling the slot)
        for (int i = 0; i < numberOfBookings; i++) {
            DarshanBookingResponseDto response =
                    darshanBookingService.createBooking(
                            DarshanBookingRequestDto.builder()
                                    .darshanSlotId(darshanSlot.getId())
                                    .devoteeName("Devotee " + i)
                                    .phoneNumber("9876543210")
                                    .ticketCount(2)
                                    .build());
            bookingIds.add(response.getBookingId());
        }

        DarshanSlot fullyBookedSlot =
                darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        assertThat(fullyBookedSlot.getBookedCount()).isEqualTo(10);
        assertThat(fullyBookedSlot.getAvailableCount()).isEqualTo(0);

        // Cancel all 5 bookings concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfBookings);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfBookings);
        AtomicInteger successCount = new AtomicInteger(0);

        for (UUID bookingId : bookingIds) {
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            darshanBookingService.cancelBooking(bookingId);
                            successCount.incrementAndGet();
                        } catch (Exception ignored) {
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(numberOfBookings);

        DarshanSlot releasedSlot =
                darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        assertThat(releasedSlot.getBookedCount()).isEqualTo(0);
        assertThat(releasedSlot.getAvailableCount()).isEqualTo(10);

        for (UUID bookingId : bookingIds) {
            DarshanBooking booking = darshanBookingRepository.findById(bookingId).orElseThrow();
            assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        }
    }

    @Test
    @DisplayName(
            "P4: Concurrent duplicate cancellations of the same booking are idempotent and release"
                    + " tickets exactly once")
    void concurrentDuplicateCancellations_ShouldBeIdempotentAndReleaseTicketsOnce()
            throws InterruptedException {
        // Create 1 booking of 3 tickets
        DarshanBookingResponseDto response =
                darshanBookingService.createBooking(
                        DarshanBookingRequestDto.builder()
                                .darshanSlotId(darshanSlot.getId())
                                .devoteeName("Devotee Single")
                                .phoneNumber("9876543210")
                                .ticketCount(3)
                                .build());
        UUID targetBookingId = response.getBookingId();

        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            darshanBookingService.cancelBooking(targetBookingId);
                            successCount.incrementAndGet();
                        } catch (Exception ignored) {
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // All threads should succeed (either primary cancel or idempotent return)
        assertThat(successCount.get()).isEqualTo(numberOfThreads);

        DarshanSlot finalSlot = darshanSlotRepository.findById(darshanSlot.getId()).orElseThrow();
        // Booked count was 3, after cancel should be 0 (NOT decremented 10 times below zero)
        assertThat(finalSlot.getBookedCount()).isEqualTo(0);
        assertThat(finalSlot.getAvailableCount()).isEqualTo(10);

        DarshanBooking cancelledBooking =
                darshanBookingRepository.findById(targetBookingId).orElseThrow();
        assertThat(cancelledBooking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName(
            "P5: Concurrent bookings do not generate duplicate receipt numbers and maintain"
                    + " strictly sequential numbering")
    void concurrentBookings_ShouldGenerateUniqueReceiptNumbers() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        java.util.Set<String> generatedReceipts =
                java.util.concurrent.ConcurrentHashMap.newKeySet();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            executorService.submit(
                    () -> {
                        try {
                            startLatch.await();
                            DarshanBookingResponseDto response =
                                    darshanBookingService.createBooking(
                                            DarshanBookingRequestDto.builder()
                                                    .darshanSlotId(darshanSlot.getId())
                                                    .devoteeName("Devotee " + index)
                                                    .phoneNumber("9876543210")
                                                    .ticketCount(1)
                                                    .build());
                            if (response.getReceiptNumber() != null) {
                                generatedReceipts.add(response.getReceiptNumber());
                                successCount.incrementAndGet();
                            }
                        } catch (Exception ignored) {
                        } finally {
                            endLatch.countDown();
                        }
                    });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        // Every concurrent booking must have received a distinct, unique receipt number
        assertThat(generatedReceipts).hasSize(numberOfThreads);

        String currentFy = receiptNumberService.calculateFinancialYear(LocalDate.now());
        String expectedPrefix = temple.getTempleCode() + "/" + currentFy + "/";

        for (String receipt : generatedReceipts) {
            assertThat(receipt).startsWith(expectedPrefix);
        }

        // Sequence row counter must equal total bookings
        Optional<TempleReceiptSequence> sequenceOpt =
                templeReceiptSequenceRepository.findByTempleIdAndFinancialYear(
                        temple.getId(), currentFy);
        assertThat(sequenceOpt).isPresent();
        assertThat(sequenceOpt.get().getLastSequenceNumber()).isEqualTo((long) numberOfThreads);
    }

    @Test
    @DisplayName(
            "P5: Transaction rollback does not consume a sequence number (counter is restored)")
    void transactionRollback_ShouldRestoreSequenceCounter() {
        String currentFy = receiptNumberService.calculateFinancialYear(LocalDate.now());

        // 1. Initial valid booking -> gets sequence 1 (000001)
        DarshanBookingResponseDto booking1 =
                darshanBookingService.createBooking(
                        DarshanBookingRequestDto.builder()
                                .darshanSlotId(darshanSlot.getId())
                                .devoteeName("Devotee One")
                                .phoneNumber("9876543210")
                                .ticketCount(2)
                                .build());
        assertThat(booking1.getReceiptNumber()).endsWith("/000001");

        // Verify sequence counter in database is 1
        TempleReceiptSequence sequenceBeforeFail =
                templeReceiptSequenceRepository
                        .findByTempleIdAndFinancialYear(temple.getId(), currentFy)
                        .orElseThrow();
        assertThat(sequenceBeforeFail.getLastSequenceNumber()).isEqualTo(1L);

        // 2. Attempt booking that exceeds capacity (remaining = 8, requesting = 15) -> throws
        // InsufficientCapacityException
        assertThatThrownBy(
                        () ->
                                darshanBookingService.createBooking(
                                        DarshanBookingRequestDto.builder()
                                                .darshanSlotId(darshanSlot.getId())
                                                .devoteeName("Devotee Overbook")
                                                .phoneNumber("9876543210")
                                                .ticketCount(15)
                                                .build()))
                .isInstanceOf(InsufficientCapacityException.class);

        // Verify sequence counter was rolled back and is STILL 1
        TempleReceiptSequence sequenceAfterFail =
                templeReceiptSequenceRepository
                        .findByTempleIdAndFinancialYear(temple.getId(), currentFy)
                        .orElseThrow();
        assertThat(sequenceAfterFail.getLastSequenceNumber()).isEqualTo(1L);

        // 3. Next successful booking receives sequence 2 (000002), proving sequence 2 was not
        // skipped
        DarshanBookingResponseDto booking2 =
                darshanBookingService.createBooking(
                        DarshanBookingRequestDto.builder()
                                .darshanSlotId(darshanSlot.getId())
                                .devoteeName("Devotee Two")
                                .phoneNumber("9876543210")
                                .ticketCount(2)
                                .build());
        assertThat(booking2.getReceiptNumber()).endsWith("/000002");

        TempleReceiptSequence sequenceFinal =
                templeReceiptSequenceRepository
                        .findByTempleIdAndFinancialYear(temple.getId(), currentFy)
                        .orElseThrow();
        assertThat(sequenceFinal.getLastSequenceNumber()).isEqualTo(2L);
    }

    @Test
    @DisplayName(
            "P5: Cancelled bookings retain their receipt numbers and subsequent bookings do not"
                    + " reuse them")
    void cancelledBooking_ShouldRetainReceiptNumber_AndNeverReuse() {
        // 1. Create booking -> gets sequence 1
        DarshanBookingResponseDto booking1 =
                darshanBookingService.createBooking(
                        DarshanBookingRequestDto.builder()
                                .darshanSlotId(darshanSlot.getId())
                                .devoteeName("Devotee ToCancel")
                                .phoneNumber("9876543210")
                                .ticketCount(2)
                                .build());
        String receipt1 = booking1.getReceiptNumber();
        assertThat(receipt1).endsWith("/000001");

        // 2. Cancel booking
        DarshanBookingResponseDto cancelled =
                darshanBookingService.cancelBooking(booking1.getBookingId());
        assertThat(cancelled.getBookingStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getReceiptNumber()).isEqualTo(receipt1);

        // Verify in repository that cancelled entity retains the receipt number
        DarshanBooking persistedCancelled =
                darshanBookingRepository.findById(booking1.getBookingId()).orElseThrow();
        assertThat(persistedCancelled.getReceiptNumber()).isEqualTo(receipt1);
        assertThat(persistedCancelled.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);

        // 3. Create another booking -> gets sequence 2 (000002), never reusing 000001
        DarshanBookingResponseDto booking2 =
                darshanBookingService.createBooking(
                        DarshanBookingRequestDto.builder()
                                .darshanSlotId(darshanSlot.getId())
                                .devoteeName("Devotee Next")
                                .phoneNumber("9876543210")
                                .ticketCount(2)
                                .build());
        String receipt2 = booking2.getReceiptNumber();
        assertThat(receipt2).endsWith("/000002");
        assertThat(receipt2).isNotEqualTo(receipt1);
    }
}
