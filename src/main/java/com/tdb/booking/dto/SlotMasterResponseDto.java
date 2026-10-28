package com.tdb.booking.dto;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotMasterResponseDto {

    private UUID id;
    private UUID templeId;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer defaultCapacity;
    private Boolean isActive;
    private Instant createdAt;
    private Instant modifiedAt;
}
