package com.tdb.booking.dto;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DarshanSlotRequestDto {

    @NotNull(message = "Temple ID is mandatory") private UUID templeId;

    @NotNull(message = "Slot master ID is mandatory") private UUID slotMasterId;

    @NotNull(message = "Slot date is mandatory") @FutureOrPresent(message = "Slot date must be today or in the future") private LocalDate slotDate;

    @NotNull(message = "Capacity is mandatory") @Positive(message = "Capacity must be greater than zero") private Integer capacity;

    @Builder.Default private Boolean isActive = true;
}
