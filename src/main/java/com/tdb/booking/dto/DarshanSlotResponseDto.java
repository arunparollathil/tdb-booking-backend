package com.tdb.booking.dto;

import java.time.Instant;
import java.time.LocalDate;
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
public class DarshanSlotResponseDto {

    private UUID id;
    private UUID templeId;
    private UUID slotMasterId;
    private LocalDate slotDate;
    private Integer capacity;
    private Integer bookedCount;
    private Integer availableCount;
    private Boolean isActive;
    private Instant createdAt;
    private Instant modifiedAt;
}
