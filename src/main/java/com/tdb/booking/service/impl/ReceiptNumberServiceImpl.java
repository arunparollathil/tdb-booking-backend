package com.tdb.booking.service.impl;

import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.entity.TempleReceiptSequence;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.repository.TempleMasterRepository;
import com.tdb.booking.repository.TempleReceiptSequenceRepository;
import com.tdb.booking.service.ReceiptNumberService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for generating unique and concurrent-safe receipt numbers.
 *
 * <p>Requirements: 1. Format: {TEMPLE_CODE}/{FINANCIAL_YEAR}/{SEQUENCE} (e.g. SBM/2026-27/000123).
 * 2. Financial year: April 1 to March 31. 3. Sequence starts at 000001 independently for each
 * temple and financial year. 4. Pessimistic row locking ensures no duplicate sequence numbers even
 * under concurrent bookings. 5. Runs within the booking creation transaction (MANDATORY
 * propagation); rollback restores the counter.
 */
@Service
@RequiredArgsConstructor
public class ReceiptNumberServiceImpl implements ReceiptNumberService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptNumberServiceImpl.class);

    private final TempleReceiptSequenceRepository templeReceiptSequenceRepository;
    private final TempleMasterRepository templeMasterRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String generateReceiptNumber(TempleMaster temple) {
        return generateReceiptNumber(temple, LocalDate.now());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String generateReceiptNumber(TempleMaster temple, LocalDate date) {
        if (temple == null || temple.getId() == null) {
            throw new InvalidRequestException(
                    "Temple reference is mandatory for receipt generation");
        }

        String financialYear = calculateFinancialYear(date);
        UUID templeId = temple.getId();

        // 1. Acquire pessimistic write lock on existing sequence row for (templeId, financialYear)
        Optional<TempleReceiptSequence> sequenceOpt =
                templeReceiptSequenceRepository.findByTempleIdAndFinancialYearForUpdate(
                        templeId, financialYear);

        long sequenceNumber;

        if (sequenceOpt.isPresent()) {
            TempleReceiptSequence sequence = sequenceOpt.get();
            sequenceNumber = sequence.getLastSequenceNumber() + 1;
            sequence.setLastSequenceNumber(sequenceNumber);
            templeReceiptSequenceRepository.save(sequence);
        } else {
            // Lock temple row to safely serialize initial sequence row creation under concurrent
            // load
            templeMasterRepository.findByIdForUpdate(templeId);

            // Re-check if another concurrent transaction inserted the row while this thread waited
            // for the lock
            sequenceOpt =
                    templeReceiptSequenceRepository.findByTempleIdAndFinancialYearForUpdate(
                            templeId, financialYear);

            if (sequenceOpt.isPresent()) {
                TempleReceiptSequence sequence = sequenceOpt.get();
                sequenceNumber = sequence.getLastSequenceNumber() + 1;
                sequence.setLastSequenceNumber(sequenceNumber);
                templeReceiptSequenceRepository.save(sequence);
            } else {
                sequenceNumber = 1L;
                TempleReceiptSequence newSequence =
                        TempleReceiptSequence.builder()
                                .temple(temple)
                                .financialYear(financialYear)
                                .lastSequenceNumber(sequenceNumber)
                                .createdBy(UUID.randomUUID())
                                .build();
                templeReceiptSequenceRepository.save(newSequence);
            }
        }

        String templeCode =
                (temple.getTempleCode() != null && !temple.getTempleCode().isBlank())
                        ? temple.getTempleCode().trim()
                        : "TDB";

        String receiptNumber =
                String.format("%s/%s/%06d", templeCode, financialYear, sequenceNumber);

        log.debug(
                "Allocated receipt number {} for temple {} (FY: {}, seq: {})",
                receiptNumber,
                templeCode,
                financialYear,
                sequenceNumber);

        return receiptNumber;
    }

    @Override
    public String calculateFinancialYear(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        int year = date.getYear();
        int month = date.getMonthValue();

        // Financial Year in India runs from April 1 to March 31
        int startYear = (month >= 4) ? year : year - 1;
        int endYearTwoDigits = (startYear + 1) % 100;

        return String.format("%d-%02d", startYear, endYearTwoDigits);
    }
}
