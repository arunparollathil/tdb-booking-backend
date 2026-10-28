package com.tdb.booking.service;

import com.tdb.booking.dto.DarshanSlotRequestDto;
import com.tdb.booking.dto.DarshanSlotResponseDto;
import java.util.UUID;

public interface DarshanSlotService {

    DarshanSlotResponseDto createDarshanSlot(DarshanSlotRequestDto requestDto);

    DarshanSlotResponseDto getDarshanSlotById(UUID slotId);
}
