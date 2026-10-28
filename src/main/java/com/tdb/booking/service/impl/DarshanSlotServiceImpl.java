package com.tdb.booking.service.impl;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.dto.DarshanSlotResponseDto;
import com.tdb.booking.entity.DarshanSlot;
import com.tdb.booking.entity.SlotMaster;
import com.tdb.booking.entity.TempleMaster;
import com.tdb.booking.exception.DuplicateResourceException;
import com.tdb.booking.exception.InvalidRequestException;
import com.tdb.booking.exception.ResourceNotFoundException;
import com.tdb.booking.exception.SlotInactiveException;
import com.tdb.booking.mapper.DarshanSlotMapper;
import com.tdb.booking.repository.DarshanSlotRepository;
import com.tdb.booking.repository.SlotMasterRepository;
import com.tdb.booking.repository.TempleMasterRepository;
import com.tdb.booking.service.DarshanSlotService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DarshanSlotServiceImpl implements DarshanSlotService {

    private static final Logger log = LoggerFactory.getLogger(DarshanSlotServiceImpl.class);

    private final TempleMasterRepository templeMasterRepository;
    private final SlotMasterRepository slotMasterRepository;
    private final DarshanSlotRepository darshanSlotRepository;
    private final DarshanSlotMapper darshanSlotMapper;

    @Override
    @Transactional
    public DarshanSlotResponseDto createDarshanSlot(DarshanSlotRequestDto requestDto) {
        log.info(
                "Creating darshan slot for templeId: {}, slotMasterId: {}, date: {}",
                requestDto.getTempleId(),
                requestDto.getSlotMasterId(),
                requestDto.getSlotDate());

        // 1. Verify temple exists and is active
        TempleMaster temple =
                templeMasterRepository
                        .findById(requestDto.getTempleId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.templeNotFound(
                                                requestDto.getTempleId()));

        if (!Boolean.TRUE.equals(temple.getIsActive())) {
            throw SlotInactiveException.templeInactive(requestDto.getTempleId());
        }

        // 2. Verify slot master exists and is active
        SlotMaster slotMaster =
                slotMasterRepository
                        .findById(requestDto.getSlotMasterId())
                        .orElseThrow(
                                () ->
                                        ResourceNotFoundException.slotMasterNotFound(
                                                requestDto.getSlotMasterId()));

        if (!Boolean.TRUE.equals(slotMaster.getIsActive())) {
            throw SlotInactiveException.slotMasterInactive(requestDto.getSlotMasterId());
        }

        // 3. Verify slot master belongs to the specified temple
        if (!slotMaster.getTemple().getId().equals(temple.getId())) {
            throw new InvalidRequestException(
                    "Slot master does not belong to the specified temple");
        }

        // 4. Check for duplicate slot for same slot master and date
        if (darshanSlotRepository.existsBySlotMasterIdAndSlotDate(
                slotMaster.getId(), requestDto.getSlotDate())) {
            throw DuplicateResourceException.duplicateSlot(
                    "A darshan slot already exists for slot master ID "
                            + slotMaster.getId()
                            + " on "
                            + requestDto.getSlotDate());
        }

        // 5. Create and persist slot
        DarshanSlot darshanSlot =
                DarshanSlot.builder()
                        .temple(temple)
                        .slotMaster(slotMaster)
                        .slotDate(requestDto.getSlotDate())
                        .capacity(requestDto.getCapacity())
                        .bookedCount(0)
                        .isActive(
                                requestDto.getIsActive() != null ? requestDto.getIsActive() : true)
                        .createdBy(UUID.randomUUID())
                        .build();

        DarshanSlot savedSlot = darshanSlotRepository.save(darshanSlot);
        log.info("Successfully created darshan slot with id: {}", savedSlot.getId());

        return darshanSlotMapper.toResponseDto(savedSlot);
    }

    @Override
    @Transactional(readOnly = true)
    public DarshanSlotResponseDto getDarshanSlotById(UUID slotId) {
        log.info("Fetching darshan slot with id: {}", slotId);

        DarshanSlot slot =
                darshanSlotRepository
                        .findById(slotId)
                        .orElseThrow(() -> ResourceNotFoundException.slotNotFound(slotId));

        return darshanSlotMapper.toResponseDto(slot);
    }
}
