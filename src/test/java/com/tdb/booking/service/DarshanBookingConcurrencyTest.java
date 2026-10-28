package com.tdb.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdb.booking.dto.DarshanBookingRequestDto;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.exception.InsufficientCapacityException;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.repository.SlotMasterRepository;
import com.tdb.booking.repository.TempleMasterRepository;
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

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    @Autowired private DarshanBookingRepository darshanBookingRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private TempleMasterRepository templeMasterRepository;

    private DarshanSlot darshanSlot;

    @BeforeEach
    void setUp() {
        darshanBookingRepository.deleteAll();
        darshanSlotRepository.deleteAll();
        slotMasterRepository.deleteAll();
        templeMasterRepository.deleteAll();

        TempleMaster temple =
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
}
