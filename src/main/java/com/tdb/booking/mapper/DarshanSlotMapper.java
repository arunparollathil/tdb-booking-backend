package com.tdb.booking.mapper;

import com.tdb.booking.dto.DarshanSlotResponseDto;
import com.tdb.booking.entity.DarshanSlot;
import org.springframework.stereotype.Component;

/**
 * Mapper component for transforming {@link DarshanSlot} entities to {@link DarshanSlotResponseDto}.
 */
@Component
public class DarshanSlotMapper {

    /**
     * Converts a {@link DarshanSlot} entity to its corresponding {@link DarshanSlotResponseDto}.
     * Populates schedule times from the linked SlotMaster and calculates available ticket count.
     *
     * @param slot the persisted darshan slot entity
     * @return populated {@link DarshanSlotResponseDto}, or null if input is null
     */
    public DarshanSlotResponseDto toResponseDto(DarshanSlot slot) {
        if (slot == null) {
            return null;
        }

        return DarshanSlotResponseDto.builder()
                .id(slot.getId())
                .templeId(slot.getTemple() != null ? slot.getTemple().getId() : null)
                .slotMasterId(slot.getSlotMaster() != null ? slot.getSlotMaster().getId() : null)
                .slotDate(slot.getSlotDate())
                .startTime(
                        slot.getSlotMaster() != null ? slot.getSlotMaster().getStartTime() : null)
                .endTime(slot.getSlotMaster() != null ? slot.getSlotMaster().getEndTime() : null)
                .capacity(slot.getCapacity())
                .bookedCount(slot.getBookedCount())
                .availableCount(slot.getAvailableCount())
                .isActive(slot.getIsActive())
                .createdAt(slot.getCreatedAt())
                .modifiedAt(slot.getModifiedAt())
                .build();
    }
}
