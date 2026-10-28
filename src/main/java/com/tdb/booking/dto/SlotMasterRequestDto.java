package com.tdb.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class SlotMasterRequestDto {

    @NotNull(message = "Temple ID is mandatory") private UUID templeId;

    @NotBlank(message = "Slot name is mandatory") @Size(max = 100, message = "Slot name must not exceed 100 characters") private String slotName;

    @NotNull(message = "Start time is mandatory") private LocalTime startTime;

    @NotNull(message = "End time is mandatory") private LocalTime endTime;

    @NotNull(message = "Default capacity is mandatory") @Positive(message = "Default capacity must be greater than zero") private Integer defaultCapacity;

    @Builder.Default private Boolean isActive = true;
}
