package com.tdb.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request DTO for creating a date-specific darshan slot. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating a new date-specific darshan slot")
public class DarshanSlotRequestDto {

    @NotNull(message = "Temple ID is mandatory") @Schema(
            description = "Unique identifier of the temple",
            example = "11111111-1111-1111-1111-111111111111",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID templeId;

    @NotNull(message = "Slot master ID is mandatory") @Schema(
            description = "Template slot master ID providing timing details",
            example = "22222222-2222-2222-2222-222222222222",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID slotMasterId;

    @NotNull(message = "Slot date is mandatory") @FutureOrPresent(message = "Slot date must be today or in the future") @Schema(
            description = "Date for which the darshan slot is created",
            example = "2026-11-01",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate slotDate;

    @NotNull(message = "Capacity is mandatory") @Positive(message = "Capacity must be greater than zero") @Schema(
            description = "Maximum ticket capacity for this slot on the specified date",
            example = "10",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer capacity;

    @Builder.Default
    @Schema(
            description = "Flag indicating whether this slot is available for bookings",
            example = "true",
            defaultValue = "true")
    private Boolean isActive = true;
}
