package com.tdb.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request DTO for booking tickets for a specific darshan slot. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for booking tickets on a darshan slot")
public class DarshanBookingRequestDto {

    @NotNull(message = "Darshan slot ID is mandatory") @Schema(
            description = "Unique identifier of the darshan slot to book",
            example = "33333333-3333-3333-3333-333333333333",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID darshanSlotId;

    @NotBlank(message = "Devotee name is mandatory") @Size(max = 150, message = "Devotee name must not exceed 150 characters") @Schema(
            description = "Full name of the devotee booking the ticket",
            example = "Arun",
            maxLength = 150,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String devoteeName;

    @NotBlank(message = "Phone number is mandatory") @Size(max = 15, message = "Phone number must not exceed 15 characters") @Pattern(regexp = "^[0-9+() -]{7,15}$", message = "Invalid phone number format")
    @Schema(
            description = "Contact phone number for confirmation and verification",
            example = "9876543210",
            maxLength = 15,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotNull(message = "Ticket count is mandatory") @Positive(message = "Ticket count must be greater than zero") @Schema(
            description = "Number of tickets to book",
            example = "3",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer ticketCount;
}
