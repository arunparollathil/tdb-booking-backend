package com.tdb.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.entity.TempleReceiptSequence;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.repository.DarshanBookingRepository;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.repository.SlotMasterRepository;
import com.tdb.booking.repository.TempleMasterRepository;
import com.tdb.booking.repository.TempleReceiptSequenceRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit & Integration tests for {@link ReceiptNumberService}. Verifies P5 requirements: 1. Financial
 * year calculation from April 1 to March 31. 2. Receipt format
 * {TEMPLE_CODE}/{FINANCIAL_YEAR}/{SEQUENCE} (e.g. SBM/2026-27/000123). 3. Independent sequences
 * starting at 000001 for each temple and financial year.
 */
@SpringBootTest
@Transactional
class ReceiptNumberServiceTest {

    @Autowired private ReceiptNumberService receiptNumberService;

    @Autowired private DarshanBookingRepository darshanBookingRepository;

    @Autowired private DarshanSlotRepository darshanSlotRepository;

    @Autowired private SlotMasterRepository slotMasterRepository;

    @Autowired private TempleReceiptSequenceRepository templeReceiptSequenceRepository;

    @Autowired private TempleMasterRepository templeMasterRepository;

    private TempleMaster sabarimalaTemple;
    private TempleMaster guruvayurTemple;

    @BeforeEach
    void setUp() {
        darshanBookingRepository.deleteAll();
        darshanSlotRepository.deleteAll();
        slotMasterRepository.deleteAll();
        templeReceiptSequenceRepository.deleteAll();
        templeMasterRepository.deleteAll();

        sabarimalaTemple =
                templeMasterRepository.save(
                        TempleMaster.builder()
                                .templeCode("SBM")
                                .templeName("Sabarimala Temple")
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());

        guruvayurTemple =
                templeMasterRepository.save(
                        TempleMaster.builder()
                                .templeCode("GUR")
                                .templeName("Guruvayur Temple")
                                .isActive(true)
                                .createdBy(UUID.randomUUID())
                                .build());
    }

    @ParameterizedTest(name = "Date {0} should map to Financial Year {1}")
    @CsvSource({
        "2026-04-01, 2026-27", // Start of FY 2026-27
        "2026-10-28, 2026-27", // Middle of FY 2026-27
        "2026-12-31, 2026-27", // End of calendar year 2026
        "2027-01-01, 2026-27", // Start of calendar year 2027 (still FY 2026-27)
        "2027-03-31, 2026-27", // Last day of FY 2026-27
        "2027-04-01, 2027-28", // First day of FY 2027-28
        "2025-03-31, 2024-25", // Prior FY
        "2099-04-01, 2099-00" // Century transition
    })
    @DisplayName("P5: Financial Year calculation strictly covers April to March")
    void calculateFinancialYear_ShouldFollowAprilToMarch(String dateStr, String expectedFy) {
        LocalDate date = LocalDate.parse(dateStr);
        String calculatedFy = receiptNumberService.calculateFinancialYear(date);
        assertThat(calculatedFy).isEqualTo(expectedFy);
    }

    @Test
    @DisplayName("P5: Receipt number follows {TEMPLE_CODE}/{FINANCIAL_YEAR}/{SEQUENCE} format")
    void generateReceiptNumber_FormatVerification() {
        LocalDate date = LocalDate.of(2026, 10, 28);
        String receiptNumber = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);

        assertThat(receiptNumber).isEqualTo("SBM/2026-27/000001");
    }

    @Test
    @DisplayName("P5: Sequential numbering increments sequentially per temple and financial year")
    void generateReceiptNumber_SequentialIncrement() {
        LocalDate date = LocalDate.of(2026, 11, 1);

        String r1 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);
        String r2 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);
        String r3 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);

        assertThat(r1).isEqualTo("SBM/2026-27/000001");
        assertThat(r2).isEqualTo("SBM/2026-27/000002");
        assertThat(r3).isEqualTo("SBM/2026-27/000003");

        Optional<TempleReceiptSequence> sequenceOpt =
                templeReceiptSequenceRepository.findByTempleIdAndFinancialYear(
                        sabarimalaTemple.getId(), "2026-27");
        assertThat(sequenceOpt).isPresent();
        assertThat(sequenceOpt.get().getLastSequenceNumber()).isEqualTo(3L);
    }

    @Test
    @DisplayName("P5: Different temples maintain strictly independent sequences")
    void generateReceiptNumber_IndependentSequencesAcrossTemples() {
        LocalDate date = LocalDate.of(2026, 10, 28);

        String sbm1 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);
        String sbm2 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, date);

        String gur1 = receiptNumberService.generateReceiptNumber(guruvayurTemple, date);
        String gur2 = receiptNumberService.generateReceiptNumber(guruvayurTemple, date);

        assertThat(sbm1).isEqualTo("SBM/2026-27/000001");
        assertThat(sbm2).isEqualTo("SBM/2026-27/000002");

        // Guruvayur temple starts at 000001 independently
        assertThat(gur1).isEqualTo("GUR/2026-27/000001");
        assertThat(gur2).isEqualTo("GUR/2026-27/000002");
    }

    @Test
    @DisplayName("P5: Sequences reset to 000001 in a new financial year")
    void generateReceiptNumber_ResetsOnNewFinancialYear() {
        LocalDate fy26 = LocalDate.of(2026, 10, 28);
        LocalDate fy27 = LocalDate.of(2027, 4, 1);

        String r2026_1 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, fy26);
        String r2026_2 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, fy26);

        // Next FY starts at 000001
        String r2027_1 = receiptNumberService.generateReceiptNumber(sabarimalaTemple, fy27);

        assertThat(r2026_1).isEqualTo("SBM/2026-27/000001");
        assertThat(r2026_2).isEqualTo("SBM/2026-27/000002");
        assertThat(r2027_1).isEqualTo("SBM/2027-28/000001");
    }

    @Test
    @DisplayName("P5: Reject receipt generation if temple is null")
    void generateReceiptNumber_NullTemple_ThrowsException() {
        assertThatThrownBy(() -> receiptNumberService.generateReceiptNumber(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Temple reference is mandatory");
    }
}
