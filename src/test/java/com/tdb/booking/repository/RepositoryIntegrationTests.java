package com.tdb.booking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tdb.booking.entity.DarshanBooking;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.entity.TempleReceiptSequence;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RepositoryIntegrationTests {

    @Autowired private TempleMasterRepository templeMasterRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    @Autowired private DarshanBookingRepository darshanBookingRepository;

    @Autowired private TempleReceiptSequenceRepository templeReceiptSequenceRepository;

    @Test
    @DisplayName("Should successfully persist and retrieve entities across all 4 tables")
    void shouldPersistAndRetrieveEntities() {
        UUID adminUserId = UUID.randomUUID();

        // 1. TempleMaster
        TempleMaster temple =
                TempleMaster.builder()
                        .templeCode("TIRUPATI_001")
                        .templeName("Sri Venkateswara Swamy Temple")
                        .description("Main hill shrine at Tirumala")
                        .isActive(true)
                        .createdBy(adminUserId)
                        .build();
        TempleMaster savedTemple = templeMasterRepository.save(temple);

        assertThat(savedTemple.getId()).isNotNull();
        assertThat(savedTemple.getCreatedAt()).isNotNull();
        assertThat(templeMasterRepository.findByTempleCode("TIRUPATI_001")).isPresent();
        assertThat(templeMasterRepository.existsByTempleCode("TIRUPATI_001")).isTrue();

        // 2. SlotMaster
        SlotMaster slot =
                SlotMaster.builder()
                        .temple(savedTemple)
                        .slotName("Morning Suprabhatham")
                        .startTime(LocalTime.of(6, 0))
                        .endTime(LocalTime.of(8, 0))
                        .defaultCapacity(500)
                        .isActive(true)
                        .createdBy(adminUserId)
                        .build();
        SlotMaster savedSlot = slotMasterRepository.save(slot);

        assertThat(savedSlot.getId()).isNotNull();
        assertThat(slotMasterRepository.findByTempleId(savedTemple.getId())).hasSize(1);
        assertThat(
                        slotMasterRepository.existsByTempleIdAndSlotNameAndStartTime(
                                savedTemple.getId(), "Morning Suprabhatham", LocalTime.of(6, 0)))
                .isTrue();

        // 3. DarshanSlot
        LocalDate visitDate = LocalDate.now().plusDays(5);
        DarshanSlot darshanSlot =
                DarshanSlot.builder()
                        .temple(savedTemple)
                        .slotMaster(savedSlot)
                        .slotDate(visitDate)
                        .capacity(500)
                        .bookedCount(50)
                        .isActive(true)
                        .createdBy(adminUserId)
                        .build();
        DarshanSlot savedDarshanSlot = darshanSlotRepository.save(darshanSlot);

        assertThat(savedDarshanSlot.getId()).isNotNull();
        assertThat(savedDarshanSlot.getAvailableCount()).isEqualTo(450);

        Optional<DarshanSlot> fetchedDarshanSlot =
                darshanSlotRepository.findBySlotMasterIdAndSlotDate(savedSlot.getId(), visitDate);
        assertThat(fetchedDarshanSlot).isPresent();

        // 4. DarshanBooking with receiptNumber and idempotencyKey
        UUID devoteeUserId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        DarshanBooking booking =
                DarshanBooking.builder()
                        .darshanSlot(savedDarshanSlot)
                        .idempotencyKey(idempotencyKey)
                        .receiptNumber("TIRUPATI_001/2026-27/000001")
                        .devoteeName("Arun Kumar")
                        .phoneNumber("+919876543210")
                        .ticketCount(4)
                        .createdBy(devoteeUserId)
                        .build();
        DarshanBooking savedBooking = darshanBookingRepository.save(booking);

        assertThat(savedBooking.getId()).isNotNull();
        assertThat(savedBooking.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(savedBooking.getReceiptNumber()).isEqualTo("TIRUPATI_001/2026-27/000001");
        List<DarshanBooking> bookings =
                darshanBookingRepository.findByDarshanSlotId(savedDarshanSlot.getId());
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getDevoteeName()).isEqualTo("Arun Kumar");
        assertThat(bookings.get(0).getTicketCount()).isEqualTo(4);

        List<DarshanBooking> bookingsByPhone =
                darshanBookingRepository.findByPhoneNumber("+919876543210");
        assertThat(bookingsByPhone).hasSize(1);

        Optional<DarshanBooking> bookingByIdempotency =
                darshanBookingRepository.findByIdempotencyKey(idempotencyKey);
        assertThat(bookingByIdempotency).isPresent();
        assertThat(darshanBookingRepository.existsByIdempotencyKey(idempotencyKey)).isTrue();

        Optional<DarshanBooking> bookingByReceipt =
                darshanBookingRepository.findByReceiptNumber("TIRUPATI_001/2026-27/000001");
        assertThat(bookingByReceipt).isPresent();
        assertThat(darshanBookingRepository.existsByReceiptNumber("TIRUPATI_001/2026-27/000001"))
                .isTrue();

        // 5. TempleReceiptSequence
        TempleReceiptSequence sequence =
                TempleReceiptSequence.builder()
                        .temple(savedTemple)
                        .financialYear("2026-27")
                        .lastSequenceNumber(1L)
                        .createdBy(adminUserId)
                        .build();
        TempleReceiptSequence savedSequence = templeReceiptSequenceRepository.save(sequence);

        assertThat(savedSequence.getId()).isNotNull();
        assertThat(savedSequence.getLastSequenceNumber()).isEqualTo(1L);

        Optional<TempleReceiptSequence> fetchedSequence =
                templeReceiptSequenceRepository.findByTempleIdAndFinancialYear(
                        savedTemple.getId(), "2026-27");
        assertThat(fetchedSequence).isPresent();
        assertThat(fetchedSequence.get().getLastSequenceNumber()).isEqualTo(1L);
    }
}
