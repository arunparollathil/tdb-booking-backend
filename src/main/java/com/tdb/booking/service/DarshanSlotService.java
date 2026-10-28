package com.tdb.booking.service;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.dto.DarshanSlotResponseDto;
import java.util.UUID;

/** Service interface for managing darshan slots and schedules. */
public interface DarshanSlotService {

    /**
     * Creates a new darshan slot for a given date, temple, and template slot master.
     *
     * @param requestDto slot creation payload
     * @return created {@link DarshanSlotResponseDto} with initialized counts
     */
    DarshanSlotResponseDto createDarshanSlot(DarshanSlotRequestDto requestDto);

    /**
     * Retrieves darshan slot details and remaining available tickets by slot ID.
     *
     * @param slotId unique identifier of the darshan slot
     * @return {@link DarshanSlotResponseDto}
     */
    DarshanSlotResponseDto getDarshanSlotById(UUID slotId);
}
