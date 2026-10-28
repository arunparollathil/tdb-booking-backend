package com.tdb.booking.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SlotInactiveException extends DarshanBookingException {

    public SlotInactiveException(String errorCode, String message) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }

    public static SlotInactiveException slotInactive(UUID slotId) {
        return new SlotInactiveException(
                "SLOT_INACTIVE", "Darshan slot is inactive with id: " + slotId);
    }

    public static SlotInactiveException templeInactive(UUID templeId) {
        return new SlotInactiveException(
                "TEMPLE_INACTIVE", "Temple is inactive with id: " + templeId);
    }

    public static SlotInactiveException slotMasterInactive(UUID slotMasterId) {
        return new SlotInactiveException(
                "SLOT_MASTER_INACTIVE", "Slot master is inactive with id: " + slotMasterId);
    }
}
