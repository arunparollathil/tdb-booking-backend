package com.tdb.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class DarshanBookingRequestDto {

    @NotNull(message = "Darshan slot ID is mandatory") private UUID darshanSlotId;

    @NotBlank(message = "Devotee name is mandatory") @Size(max = 150, message = "Devotee name must not exceed 150 characters") private String devoteeName;

    @NotBlank(message = "Phone number is mandatory") @Size(max = 15, message = "Phone number must not exceed 15 characters") private String phoneNumber;

    @NotNull(message = "Ticket count is mandatory") @Positive(message = "Ticket count must be greater than zero") private Integer ticketCount;
}
