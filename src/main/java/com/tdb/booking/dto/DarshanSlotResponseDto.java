package com.tdb.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Response DTO providing darshan slot details, schedule, and availability. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Details of a darshan slot including schedule and real-time availability")
public class DarshanSlotResponseDto {

    @Schema(
            description = "Unique identifier of the darshan slot",
            example = "33333333-3333-3333-3333-333333333333")
    private UUID id;

    @Schema(
            description = "Unique identifier of the associated temple",
            example = "11111111-1111-1111-1111-111111111111")
    private UUID templeId;

    @Schema(
            description = "Template slot master ID",
            example = "22222222-2222-2222-2222-222222222222")
    private UUID slotMasterId;

    @Schema(description = "Date of the darshan slot", example = "2026-11-01")
    private LocalDate slotDate;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(
            description = "Start time of the darshan slot (from SlotMaster)",
            example = "09:00:00",
            type = "string")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(
            description = "End time of the darshan slot (from SlotMaster)",
            example = "10:00:00",
            type = "string")
    private LocalTime endTime;

    @Schema(description = "Total capacity of the slot", example = "10")
    private Integer capacity;

    @Schema(description = "Total number of tickets booked so far", example = "0")
    private Integer bookedCount;

    @Schema(
            description = "Remaining tickets available for booking (capacity - bookedCount)",
            example = "10")
    private Integer availableCount;

    @Schema(description = "Whether the slot is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Last modification timestamp")
    private Instant modifiedAt;
}
